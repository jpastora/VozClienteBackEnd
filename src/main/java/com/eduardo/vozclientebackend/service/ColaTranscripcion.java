package com.eduardo.vozclientebackend.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * "Semáforo" de las transcripciones: cola FIFO atendida por UN solo hilo.
 *
 * <p>Mientras una transcripción está en proceso (la petición a Whisper sigue abierta) las demás
 * esperan en memoria; nunca se le envían dos audios a la vez. Así tampoco se agota el
 * {@code readTimeout} del cliente HTTP por esperar turno en el servidor de Whisper.</p>
 *
 * <p>La cola vive en memoria: si el backend se reinicia, {@link TranscripcionService} la reconstruye
 * desde la base de datos (estados PENDIENTE / EN_PROCESO).</p>
 */
@Slf4j
@Component
public class ColaTranscripcion {

    /** Un único hilo trabajador + cola ilimitada = una transcripción a la vez, en orden de llegada. */
    private final ThreadPoolExecutor trabajador = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), hilo("cola-transcripcion"));

    /** Solo programa los reintentos con espera; el trabajo pesado lo hace siempre {@link #trabajador}. */
    private final ScheduledThreadPoolExecutor reloj = new ScheduledThreadPoolExecutor(1, hilo("reintento-transcripcion"));

    /** Ids que ya esperan turno, para no encolar dos veces la misma transcripción. */
    private final Set<Long> enEspera = ConcurrentHashMap.newKeySet();

    /**
     * Pone la transcripción a la cola.
     *
     * @return {@code false} si ya estaba esperando turno (no se duplica) o la cola está cerrada
     */
    public boolean encolar(Long transcripcionId, Consumer<Long> procesador) {
        if (!enEspera.add(transcripcionId)) {
            return false;
        }
        try {
            trabajador.execute(() -> {
                enEspera.remove(transcripcionId);
                try {
                    procesador.accept(transcripcionId);
                } catch (Throwable e) {
                    // Un fallo inesperado no debe matar al hilo ni bloquear a las demás.
                    log.error("Fallo inesperado procesando la transcripción {}", transcripcionId, e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            enEspera.remove(transcripcionId);
            log.warn("Cola de transcripción cerrada; no se encoló la transcripción {}", transcripcionId);
            return false;
        }
    }

    /** Vuelve a encolar la transcripción pasado el tiempo indicado (espera entre reintentos). */
    public void encolarDespues(Long transcripcionId, Duration espera, Consumer<Long> procesador) {
        try {
            reloj.schedule(() -> encolar(transcripcionId, procesador), espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            log.warn("No se pudo programar el reintento de la transcripción {}", transcripcionId);
        }
    }

    /** Transcripciones esperando turno (sin contar la que está en proceso). */
    public int enEspera() {
        return trabajador.getQueue().size();
    }

    @PreDestroy
    void cerrar() {
        reloj.shutdownNow();
        // Lo que estaba en cola o en proceso queda en BD como PENDIENTE / EN_PROCESO y se recupera al arrancar.
        trabajador.shutdownNow();
    }

    private static ThreadFactory hilo(String nombre) {
        return r -> {
            Thread t = new Thread(r, nombre);
            t.setDaemon(true);
            return t;
        };
    }
}

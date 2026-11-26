package com.eduardo.vozclientebackend.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
public class ColaAnalisis {

    private final ThreadPoolExecutor trabajador =
        new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread hilo = new Thread(runnable, "cola-analisis");
                hilo.setDaemon(true);
                return hilo;
            }
        );

    private final Set<Long> registrados =
        ConcurrentHashMap.newKeySet();

    public boolean encolar(
        Long transcripcionId,
        Consumer<Long> procesador
    ) {
        if (!registrados.add(transcripcionId)) {
            return false;
        }

        try {
            trabajador.execute(() -> {
                try {
                    procesador.accept(transcripcionId);
                } catch (Exception e) {
                    log.error(
                        "Error procesando análisis de transcripción {}",
                        transcripcionId,
                        e
                    );
                } finally {
                    registrados.remove(transcripcionId);
                }
            });

            return true;

        } catch (RejectedExecutionException e) {
            registrados.remove(transcripcionId);

            log.warn(
                "Cola de análisis cerrada. Transcripción: {}",
                transcripcionId
            );

            return false;
        }
    }

    public int enEspera() {
        return trabajador.getQueue().size();
    }

    @PreDestroy
    void cerrar() {
        trabajador.shutdownNow();
    }
}

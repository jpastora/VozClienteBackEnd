package com.eduardo.vozclientebackend.scheduler;

import com.eduardo.vozclientebackend.service.GrabacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;


/**
 * Cierra como parcial las grabaciones que quedaron en GRABANDO porque el cliente se desconectó
 * sin poder avisar (HU-10). El servidor no depende de que el navegador finalice la grabación.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrabacionScheduler {

    private final GrabacionService grabacionService;

    /** Segundos sin recibir fragmentos para dar la grabación por abandonada (la reconexión del front dura 30 s). */
    @Value("${app.grabacion.inactividad-segundos:60}")
    private long inactividadSegundos;

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciarServidor() {
        cerrarGrabacionesInactivas();
    }

    @Scheduled(fixedDelay = 30_000)
    public void cerrarGrabacionesInactivas() {
        for (Long entrevistaId : grabacionService.entrevistasConGrabacionInactiva(Duration.ofSeconds(inactividadSegundos))) {
            try {
                grabacionService.cerrarPorInactividad(entrevistaId);
            } catch (RuntimeException e) {
                log.error("No se pudo cerrar la grabación inactiva de la entrevista {}", entrevistaId, e);
            }
        }
    }
}


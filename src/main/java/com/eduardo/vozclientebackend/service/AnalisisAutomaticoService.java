package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import com.eduardo.vozclientebackend.models.Analisis;
import com.eduardo.vozclientebackend.repository.AnalisisRepository;
import com.eduardo.vozclientebackend.repository.TranscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalisisAutomaticoService {

    private final AnalisisService analisisService;
    private final AnalisisRepository analisisRepository;
    private final TranscripcionRepository transcripcionRepository;
    private final ColaAnalisis colaAnalisis;

    @Value("${analisis.auto:true}")
    private boolean autoHabilitado;

    @Value("${analisis.max-intentos:3}")
    private int maxIntentos;

    @Value("${analisis.espera-reintento-segundos:60}")
    private long esperaReintentoSegundos;

    public void solicitarAutomatico(Long transcripcionId) {

        if (!autoHabilitado) {
            return;
        }

        try {
            if (colaAnalisis.encolar(
                transcripcionId,
                this::procesar
            )) {
                log.info(
                    "Análisis de transcripción {} puesto en cola",
                    transcripcionId
                );
            }

        } catch (Exception e) {
            log.error(
                "No se pudo solicitar análisis automático de {}",
                transcripcionId,
                e
            );
        }
    }

    private void procesar(Long transcripcionId) {

        var transcripcion = transcripcionRepository
            .findById(transcripcionId)
            .orElse(null);

        if (transcripcion == null
            || transcripcion.getEstado()
            != EstadoTranscripcion.COMPLETADA
            || transcripcion.getTexto() == null
            || transcripcion.getTexto().isBlank()) {

            return;
        }

        Analisis existente = analisisRepository
            .findByTranscripcionId(transcripcionId)
            .orElse(null);

        if (existente != null) {

            if (existente.getEstado()
                == EstadoAnalisis.COMPLETADA) {

                return;
            }

            if (existente.getIntentos() >= maxIntentos) {
                return;
            }

            if (existente.getEstado()
                == EstadoAnalisis.CON_ERROR
                && !puedeReintentar(existente)) {

                return;
            }
        }

        try {
            log.info(
                "Iniciando análisis Gemini de transcripción {}",
                transcripcionId
            );

            var resultado =
                analisisService.procesarAnalisis(transcripcionId);

            log.info(
                "Finalizó la solicitud de análisis de transcripción {}",
                transcripcionId
            );

        } catch (Exception e) {
            log.error(
                "Error ejecutando análisis de transcripción {}",
                transcripcionId,
                e
            );
        }
    }

    private boolean puedeReintentar(Analisis analisis) {

        if (analisis.getIntentos() >= maxIntentos) {
            return false;
        }

        if (analisis.getActualizadoEn() == null) {
            return true;
        }

        OffsetDateTime siguienteIntento =
            analisis.getActualizadoEn()
                .plusSeconds(esperaReintentoSegundos);

        return !OffsetDateTime.now()
            .isBefore(siguienteIntento);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarAlArrancar() {

        if (!autoHabilitado) {
            return;
        }

        log.info("Recuperando análisis pendientes");

        recuperarPendientes();
    }

    @Scheduled(
        fixedDelayString =
            "${analisis.intervalo-recuperacion-ms:30000}"
    )
    public void recuperarPendientes() {

        if (!autoHabilitado) {
            return;
        }

        try {
            transcripcionRepository
                .buscarIdsSinAnalisis(
                    EstadoTranscripcion.COMPLETADA
                )
                .forEach(this::solicitarAutomatico);

            analisisRepository
                .findByEstado(EstadoAnalisis.PENDIENTE)
                .forEach(a ->
                    solicitarAutomatico(a.getTranscripcionId())
                );

            analisisRepository
                .findByEstado(EstadoAnalisis.EN_PROCESO)
                .forEach(a ->
                    solicitarAutomatico(a.getTranscripcionId())
                );

            analisisRepository
                .findByEstado(EstadoAnalisis.CON_ERROR)
                .stream()
                .filter(this::puedeReintentar)
                .forEach(a ->
                    solicitarAutomatico(a.getTranscripcionId())
                );

        } catch (Exception e) {
            log.error(
                "No se pudieron recuperar los análisis pendientes",
                e
            );
        }
    }
}

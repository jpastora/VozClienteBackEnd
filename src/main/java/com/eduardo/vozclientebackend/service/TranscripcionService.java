package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.TranscripcionDTO;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import com.eduardo.vozclientebackend.mappers.TranscripcionMapper;
import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.exceptions.GrabacionEstadoInvalidoException;
import com.eduardo.vozclientebackend.models.Grabacion;
import com.eduardo.vozclientebackend.models.Transcripcion;
import com.eduardo.vozclientebackend.repository.GrabacionRepository;
import com.eduardo.vozclientebackend.repository.TranscripcionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class TranscripcionService {

    @Value("${whisper.api.url:http://localhost:8000/transcribir}")
    private String whisperApiUrl;

    /** Transcribir automáticamente al cerrarse la grabación. */
    @Value("${transcripcion.auto:true}")
    private boolean autoHabilitada;

    /** Intentos automáticos por transcripción (la tabla documenta un máximo de 3). */
    @Value("${transcripcion.max-intentos:3}")
    private int maxIntentos;

    /** Espera antes del 2.º intento; se duplica en cada reintento (30 s, 60 s, ...). */
    @Value("${transcripcion.espera-reintento-segundos:30}")
    private long esperaReintentoSegundos;

    private static final int MAX_MENSAJE_ERROR = 500; // longitud de transcripciones.mensaje_error

    private final TranscripcionRepository transcripcionRepository;
    private final GrabacionRepository grabacionRepository;
    private final TranscripcionMapper transcripcionMapper;
    private final RestTemplate restTemplate;
    private final ColaTranscripcion cola;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AnalisisAutomaticoService analisisAutomaticoService;

    public TranscripcionService(
        TranscripcionRepository transcripcionRepository,
        GrabacionRepository grabacionRepository,
        TranscripcionMapper transcripcionMapper,
        RestTemplateBuilder restTemplateBuilder,
        ColaTranscripcion cola,
        AnalisisAutomaticoService analisisAutomaticoService
    ) {
        this.transcripcionRepository = transcripcionRepository;
        this.grabacionRepository = grabacionRepository;
        this.transcripcionMapper = transcripcionMapper;
        this.cola = cola;
        this.analisisAutomaticoService = analisisAutomaticoService;

        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofMinutes(15))
            .build();
    }

    /**
     * Crea (o reutiliza si estaba CON_ERROR) el registro de transcripción en estado PENDIENTE
     * y lanza el procesamiento en segundo plano. Devuelve inmediatamente.
     */
    public TranscripcionDTO solicitarTranscripcion(Long grabacionId) {
        Grabacion grabacion = grabacionRepository.findById(grabacionId)
            .orElseThrow(() -> new RuntimeException("Grabación no encontrada con ID: " + grabacionId));

        // El archivo todavía se está escribiendo: se transcribe cuando la grabación se cierre.
        if (grabacion.getEstado() == EstadoGrabacion.GRABANDO) {
            throw new GrabacionEstadoInvalidoException(
                "La grabación " + grabacionId + " sigue en curso; espera a que termine.");
        }

        Transcripcion transcripcion = transcripcionRepository.findByGrabacionId(grabacionId)
            .orElseGet(() -> {
                Transcripcion nueva = new Transcripcion();
                nueva.setGrabacionId(grabacionId);
                nueva.setIntentos(0);
                return nueva;
            });

        if (transcripcion.getEstado() == EstadoTranscripcion.EN_PROCESO
            || transcripcion.getEstado() == EstadoTranscripcion.COMPLETADA) {
            throw new IllegalStateException("La grabación " + grabacionId
                + " ya tiene una transcripción en estado " + transcripcion.getEstado());
        }

        // Un reintento manual (desde CON_ERROR o SIN_CONTENIDO) vuelve a dar los intentos automáticos completos.
        if (transcripcion.getEstado() != null && transcripcion.getEstado() != EstadoTranscripcion.PENDIENTE) {
            transcripcion.setIntentos(0);
        }
        transcripcion.setEstado(EstadoTranscripcion.PENDIENTE);
        transcripcion.setMensajeError(null);
        Transcripcion guardada = transcripcionRepository.save(transcripcion);

        cola.encolar(guardada.getId(), this::procesar);

        return transcripcionMapper.toDto(guardada);
    }

    // ------------------------------------------------------------------
    // Disparo automático y recuperación
    // ------------------------------------------------------------------

    /**
     * Lo llama {@code GrabacionService} al cerrar una grabación con audio: crea la transcripción
     * PENDIENTE y la pone en la cola.
     *
     * <p>Si hay una transacción en curso (el cierre de la grabación) espera a que haga commit antes de
     * actuar: así no se transcribe una grabación que luego se revierte. Nunca propaga errores:
     * transcribir es secundario y no debe hacer fallar el cierre.</p>
     */
    public void solicitarAutomatica(Long grabacionId) {
        if (!autoHabilitada) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    encolarAutomatica(grabacionId);
                }
            });
        } else {
            encolarAutomatica(grabacionId);
        }
    }

    private void encolarAutomatica(Long grabacionId) {
        try {
            // Si ya existe una transcripción (en cualquier estado) no se pisa: manda la solicitud manual.
            if (transcripcionRepository.existsByGrabacionId(grabacionId)) {
                return;
            }
            Transcripcion nueva = new Transcripcion();
            nueva.setGrabacionId(grabacionId);
            nueva.setIntentos(0);
            nueva.setEstado(EstadoTranscripcion.PENDIENTE);
            Transcripcion guardada = transcripcionRepository.save(nueva);
            cola.encolar(guardada.getId(), this::procesar);
            log.info("Transcripción {} de la grabación {} puesta en cola ({} esperando)",
                guardada.getId(), grabacionId, cola.enEspera());
        } catch (DataIntegrityViolationException e) {
            log.debug("La grabación {} ya tenía transcripción (solicitud simultánea)", grabacionId);
        } catch (Exception e) {
            log.error("No se pudo encolar la transcripción automática de la grabación {}", grabacionId, e);
        }
    }

    /**
     * La cola vive en memoria: al arrancar se reconstruye con lo que quedó sin terminar. Lo que estaba
     * EN_PROCESO se interrumpió con el apagado, así que vuelve a PENDIENTE.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recuperarPendientes() {
        try {
            List<Transcripcion> interrumpidas = transcripcionRepository.findByEstado(EstadoTranscripcion.EN_PROCESO);
            for (Transcripcion t : interrumpidas) {
                t.setEstado(EstadoTranscripcion.PENDIENTE);
                transcripcionRepository.save(t);
            }
            List<Transcripcion> pendientes = transcripcionRepository.findByEstado(EstadoTranscripcion.PENDIENTE)
                .stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
            pendientes.forEach(t -> cola.encolar(t.getId(), this::procesar));
            if (!pendientes.isEmpty()) {
                log.info("Se recuperaron {} transcripciones pendientes tras el arranque", pendientes.size());
            }
        } catch (Exception e) {
            log.error("No se pudieron recuperar las transcripciones pendientes", e);
        }
    }

    /**
     * Envía el audio a la API Whisper y guarda el resultado.
     * No es @Transactional a propósito: la llamada HTTP es larga y los cambios de estado
     * (EN_PROCESO / CON_ERROR) deben persistirse de inmediato.
     */
    void procesar(Long transcripcionId) {
        Transcripcion t = transcripcionRepository.findById(transcripcionId).orElse(null);
        if (t == null) {
            log.error("Transcripción {} no encontrada", transcripcionId);
            return;
        }
        // Puede haberse completado o ya estar en marcha por otra vía mientras esperaba turno.
        if (t.getEstado() != EstadoTranscripcion.PENDIENTE) {
            log.debug("Transcripción {} omitida: estado {}", transcripcionId, t.getEstado());
            return;
        }

        try {
            Grabacion grabacion = grabacionRepository.findById(t.getGrabacionId())
                .orElseThrow(() -> new IllegalStateException("Grabación no encontrada: " + t.getGrabacionId()));

            String ruta = grabacion.getRutaCanalCliente();
            if (ruta == null || ruta.isBlank()) {
                marcarError(t, "La grabación no tiene ruta de archivo de audio");
                return;
            }
            File archivo = new File(ruta);
            if (!archivo.exists()) {
                marcarError(t, "El archivo de audio no existe en disco: " + ruta);
                return;
            }

            t.setEstado(EstadoTranscripcion.EN_PROCESO);
            t.setIntentos(t.getIntentos() + 1);
            transcripcionRepository.save(t);

            log.info("Enviando '{}' a Whisper ({})", archivo.getName(), whisperApiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(archivo));

            ResponseEntity<String> response = restTemplate.postForEntity(
                whisperApiUrl, new HttpEntity<>(body, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                marcarError(t, "Respuesta inválida de Whisper: " + response.getStatusCode());
                return;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String texto = root.path("texto").asText("").trim();
            String idioma = root.path("idioma").asText("es");
            double prob = root.path("probabilidadIdioma").asDouble(1.0);

            t.setIdiomaDetectado(idioma);
            t.setProbabilidadIdioma(BigDecimal.valueOf(prob));
            t.setMensajeError(null);

            if (texto.isEmpty()) {
                t.setTexto(null);
                t.setEstado(EstadoTranscripcion.SIN_CONTENIDO);
            } else {
                t.setTexto(texto);
                t.setEstado(EstadoTranscripcion.COMPLETADA);
            }

            Transcripcion guardada =
                transcripcionRepository.save(t);

            if (guardada.getEstado()
                == EstadoTranscripcion.COMPLETADA) {

                analisisAutomaticoService
                    .solicitarAutomatico(guardada.getId());
            }

        } catch (HttpStatusCodeException e) {
            log.error("Error HTTP {} desde Whisper para transcripción {}: {}",
                e.getStatusCode(), transcripcionId, e.getResponseBodyAsString());
            String mensaje = "Error Whisper (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
            if (esTransitorio(e.getStatusCode())) {
                falloTransitorio(t, mensaje);
            } else {
                marcarError(t, mensaje); // 4xx: archivo inválido; repetirlo no ayuda
            }
        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con Whisper en {}: {}", whisperApiUrl, e.getMessage());
            falloTransitorio(t, "No se pudo conectar con la API de Whisper en " + whisperApiUrl);
        } catch (Exception e) {
            log.error("Error procesando transcripción {}", transcripcionId, e);
            marcarError(t, e.getMessage());
        }
    }

    private static boolean esTransitorio(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == 408 || status.value() == 429;
    }

    /**
     * Fallo que puede resolverse solo (Whisper caído o reiniciando, sobrecarga). Mientras queden
     * intentos vuelve a PENDIENTE y se reencola tras una espera creciente; al agotarlos queda CON_ERROR
     * y solo se reintenta manualmente.
     */
    private void falloTransitorio(Transcripcion t, String mensaje) {
        int intentos = t.getIntentos();
        if (intentos >= maxIntentos) {
            marcarError(t, mensaje + " (se agotaron los " + maxIntentos + " intentos)");
            return;
        }
        long espera = esperaReintentoSegundos * (1L << (intentos - 1));
        t.setEstado(EstadoTranscripcion.PENDIENTE);
        t.setMensajeError(recortar("Intento " + intentos + " de " + maxIntentos
            + " fallido; se reintenta en " + espera + " s. " + mensaje));
        transcripcionRepository.save(t);
        cola.encolarDespues(t.getId(), Duration.ofSeconds(espera), this::procesar);
        log.warn("Transcripción {}: intento {}/{} fallido, reintento en {} s", t.getId(), intentos, maxIntentos, espera);
    }

    private void marcarError(Transcripcion t, String mensaje) {
        t.setEstado(EstadoTranscripcion.CON_ERROR);
        t.setMensajeError(recortar(mensaje));
        transcripcionRepository.save(t);
    }

    /** La columna mensaje_error admite 500 caracteres; un cuerpo de error largo haría fallar el guardado. */
    private static String recortar(String mensaje) {
        if (mensaje == null || mensaje.length() <= MAX_MENSAJE_ERROR) {
            return mensaje;
        }
        return mensaje.substring(0, MAX_MENSAJE_ERROR - 1) + "…";
    }

    public TranscripcionDTO obtenerPorId(Long id) {
        return transcripcionRepository.findById(id)
            .map(transcripcionMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Transcripción no encontrada con ID: " + id));
    }

    public TranscripcionDTO obtenerPorGrabacionId(Long grabacionId) {
        return transcripcionRepository.findByGrabacionId(grabacionId)
            .map(transcripcionMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Transcripción no encontrada para la grabación ID: " + grabacionId));
    }
}

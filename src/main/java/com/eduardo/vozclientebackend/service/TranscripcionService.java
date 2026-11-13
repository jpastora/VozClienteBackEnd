package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.TranscripcionDTO;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import com.eduardo.vozclientebackend.mappers.TranscripcionMapper;
import com.eduardo.vozclientebackend.models.Grabacion;
import com.eduardo.vozclientebackend.models.Transcripcion;
import com.eduardo.vozclientebackend.repository.GrabacionRepository;
import com.eduardo.vozclientebackend.repository.TranscripcionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TranscripcionService {

    @Value("${whisper.api.url:http://localhost:8000/transcribir}")
    private String whisperApiUrl;

    private final TranscripcionRepository transcripcionRepository;
    private final GrabacionRepository grabacionRepository;
    private final TranscripcionMapper transcripcionMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscripcionService(TranscripcionRepository transcripcionRepository,
                                GrabacionRepository grabacionRepository,
                                TranscripcionMapper transcripcionMapper,
                                RestTemplateBuilder restTemplateBuilder) {
        this.transcripcionRepository = transcripcionRepository;
        this.grabacionRepository = grabacionRepository;
        this.transcripcionMapper = transcripcionMapper;
        // Whisper puede tardar varios minutos con audios largos
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
        if (!grabacionRepository.existsById(grabacionId)) {
            throw new RuntimeException("Grabación no encontrada con ID: " + grabacionId);
        }

        Transcripcion transcripcion = transcripcionRepository.findByGrabacionId(grabacionId)
            .orElseGet(() -> {
                Transcripcion nueva = new Transcripcion();
                nueva.setGrabacionId(grabacionId);
                nueva.setIntentos(1);
                return nueva;
            });

        if (transcripcion.getEstado() == EstadoTranscripcion.EN_PROCESO
            || transcripcion.getEstado() == EstadoTranscripcion.COMPLETADA) {
            throw new IllegalStateException("La grabación " + grabacionId
                + " ya tiene una transcripción en estado " + transcripcion.getEstado());
        }

        transcripcion.setEstado(EstadoTranscripcion.PENDIENTE);
        transcripcion.setMensajeError(null);
        Transcripcion guardada = transcripcionRepository.save(transcripcion);

        Long id = guardada.getId();
        CompletableFuture.runAsync(() -> procesar(id));

        return transcripcionMapper.toDto(guardada);
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
            transcripcionRepository.save(t);

        } catch (HttpStatusCodeException e) {
            log.error("Error HTTP {} desde Whisper para transcripción {}: {}",
                e.getStatusCode(), transcripcionId, e.getResponseBodyAsString());
            marcarError(t, "Error Whisper (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("No se pudo conectar con Whisper en {}: {}", whisperApiUrl, e.getMessage());
            marcarError(t, "No se pudo conectar con la API de Whisper en " + whisperApiUrl);
        } catch (Exception e) {
            log.error("Error procesando transcripción {}", transcripcionId, e);
            marcarError(t, e.getMessage());
        }
    }

    private void marcarError(Transcripcion t, String mensaje) {
        t.setEstado(EstadoTranscripcion.CON_ERROR);
        t.setMensajeError(mensaje);
        transcripcionRepository.save(t);
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

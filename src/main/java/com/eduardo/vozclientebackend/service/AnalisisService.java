package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.AnalisisDTO;
import com.eduardo.vozclientebackend.dto.MencionCompetenciaDTO;
import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.enums.SentimientoAnalisis;
import com.eduardo.vozclientebackend.mappers.AnalisisMapper;
import com.eduardo.vozclientebackend.models.Analisis;
import com.eduardo.vozclientebackend.models.MencionCompetencia;
import com.eduardo.vozclientebackend.models.Transcripcion;
import com.eduardo.vozclientebackend.repository.AnalisisRepository;
import com.eduardo.vozclientebackend.repository.MencionCompetenciaRepository;
import com.eduardo.vozclientebackend.repository.TranscripcionRepository;
import com.eduardo.vozclientebackend.mappers.MencionCompetenciaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AnalisisService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisService.class);

    private static final String SYSTEM_PROMPT = """
            Eres un analista experto en calidad de llamadas. Analiza la siguiente transcripción.
            Debes extraer los datos respetando estrictamente los siguientes valores permitidos:

            1. 'intencion': Solo puedes responder con uno de estos valores: RECOMPRARIA, NO_RECOMPRARIA, RECHAZA, NEUTRAL.
            2. 'sentimiento': Solo puedes responder con uno de estos valores: POSITIVO, NEGATIVO, NEUTRO.
            3. 'fragmentoApoyo': Una cita textual corta (máx. 1000 caracteres) de la transcripción que justifique la intención y sentimiento.
            4. 'menciones': Una lista de competidores mencionados. Cada mención debe incluir 'competidor' (nombre), 'frecuencia' (número exacto de veces que se menciona) y 'contexto' (breve resumen). Si no hay competidores, devuelve una lista vacía [].
            """;

    private final ChatClient chatClient;
    private final TranscripcionRepository transcripcionRepository;
    private final AnalisisRepository analisisRepository;
    private final MencionCompetenciaRepository mencionesRepository;
    private final AnalisisMapper analisisMapper;
    private final MencionCompetenciaMapper mencionCompetenciaMapper;

    record MencionIA(String competidor, Short frecuencia, String contexto) {}
    record RespuestaAnalisisIA(IntencionAnalisis intencion, SentimientoAnalisis sentimiento,
                               String fragmentoApoyo, List<MencionIA> menciones) {}

    public AnalisisService(ChatClient.Builder chatClientBuilder,
                           TranscripcionRepository transcripcionRepository,
                           AnalisisRepository analisisRepository,
                           MencionCompetenciaRepository mencionesRepository,
                           AnalisisMapper analisisMapper,
                           MencionCompetenciaMapper mencionCompetenciaMapper) {
        this.chatClient = chatClientBuilder.build();
        this.transcripcionRepository = transcripcionRepository;
        this.analisisRepository = analisisRepository;
        this.mencionesRepository = mencionesRepository;
        this.analisisMapper = analisisMapper;
        this.mencionCompetenciaMapper = mencionCompetenciaMapper;
    }

    @Transactional
    public AnalisisDTO procesarAnalisis(Long transcripcionId) {
        Transcripcion transcripcion = transcripcionRepository.findById(transcripcionId)
            .orElseThrow(() -> new IllegalArgumentException("Transcripción no encontrada para ID: " + transcripcionId));

        // Cambiamos el estado a EN_PROCESO si ya existía un registro previo pendiente
        Analisis analisis = analisisRepository.findByTranscripcionId(transcripcionId)
            .orElseGet(() -> {
                Analisis nuevo = new Analisis();
                nuevo.setTranscripcionId(transcripcionId);
                nuevo.setCreadoEn(OffsetDateTime.now());
                nuevo.setIntentos((short) 0);
                return nuevo;
            });

        analisis.setEstado(EstadoAnalisis.EN_PROCESO);
        analisis.setIntentos((short) (analisis.getIntentos() + 1));
        analisisRepository.save(analisis);

        RespuestaAnalisisIA respuestaIA;
        try {
            respuestaIA = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(transcripcion.getTexto())
                .call()
                .entity(RespuestaAnalisisIA.class);
        } catch (Exception e) {
            log.error("Error procesando análisis de IA para transcripción {}", transcripcionId, e);
            return registrarAnalisisFallido(analisis, e.getMessage());
        }

        // Actualizamos los datos obtenidos
        analisis.setEstado(EstadoAnalisis.COMPLETADA);
        analisis.setIntencion(respuestaIA.intencion());
        analisis.setSentimiento(respuestaIA.sentimiento());

        String fragmento = respuestaIA.fragmentoApoyo() != null && respuestaIA.fragmentoApoyo().length() > 1000
            ? respuestaIA.fragmentoApoyo().substring(0, 997) + "..." : respuestaIA.fragmentoApoyo();
        analisis.setFragmentoApoyo(fragmento);

        analisis.setProveedorIa("Google Gemini");
        analisis.setMensajeError(null);
        analisis.setActualizadoEn(OffsetDateTime.now());

        boolean tieneMenciones = respuestaIA.menciones() != null && !respuestaIA.menciones().isEmpty();
        analisis.setTieneMenciones(tieneMenciones);

        Analisis analisisGuardado = analisisRepository.save(analisis);

        // Limpiar menciones anteriores en caso de ser un reintento
        List<MencionCompetencia> mencionesPrevias = mencionesRepository.findByAnalisisId(analisisGuardado.getId());
        if (!mencionesPrevias.isEmpty()) {
            mencionesRepository.deleteAll(mencionesPrevias);
        }

        // Guardar nuevas menciones
        if (tieneMenciones) {
            List<MencionCompetencia> mencionesEntidades = respuestaIA.menciones().stream().map(m -> {
                MencionCompetencia mc = new MencionCompetencia();
                mc.setAnalisisId(analisisGuardado.getId());
                mc.setCompetidor(m.competidor());
                mc.setFrecuencia(m.frecuencia());

                String ctx = m.contexto() != null && m.contexto().length() > 500
                    ? m.contexto().substring(0, 497) + "..." : m.contexto();
                mc.setContexto(ctx);
                return mc;
            }).toList();

            mencionesRepository.saveAll(mencionesEntidades);
        }

        return analisisMapper.toDto(analisisGuardado);
    }

    private AnalisisDTO registrarAnalisisFallido(Analisis analisis, String error) {
        analisis.setEstado(EstadoAnalisis.CON_ERROR);

        String errorMsg = error != null && error.length() > 500
            ? error.substring(0, 497) + "..." : error;
        analisis.setMensajeError(errorMsg);

        analisis.setActualizadoEn(OffsetDateTime.now());
        Analisis guardado = analisisRepository.save(analisis);

        return analisisMapper.toDto(guardado);
    }

    @Transactional(readOnly = true)
    public AnalisisDTO obtenerAnalisis(Long transcripcionId) {
        return analisisRepository.findByTranscripcionId(transcripcionId)
            .map(analisisMapper::toDto)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró un análisis para la transcripción: " + transcripcionId));
    }

    @Transactional(readOnly = true)
    public List<MencionCompetenciaDTO> obtenerMencionesPorTranscripcion(Long transcripcionId) {
        Analisis analisis = analisisRepository.findByTranscripcionId(transcripcionId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró análisis para la transcripción: " + transcripcionId));

        return mencionCompetenciaMapper.toDtoList(analisis.getMenciones());
    }
}

package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.*;
import com.eduardo.vozclientebackend.models.*;
import com.eduardo.vozclientebackend.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntrevistaDetalleService {

    private final EntrevistaRepository entrevistaRepository;
    private final SalaRepository salaRepository;
    private final ProductoRepository productoRepository;
    private final CuentaRepository cuentaRepository;
    private final GrabacionRepository grabacionRepository;
    private final TranscripcionRepository transcripcionRepository;
    private final AnalisisRepository analisisRepository;
    private final FragmentoTranscripcionRepository fragmentoRepository;
    private final MencionCompetenciaRepository mencionRepository;
    private final UsuarioActualService usuarioActualService;

    @Transactional(readOnly = true)
    public EntrevistaDetalleDTO obtenerDetalle(Long id) {

        Long empresaId = usuarioActualService
            .obtenerCuentaActual()
            .getEmpresaId();

        if (empresaId == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El usuario no tiene empresa asignada");
        }

        Entrevista entrevista = entrevistaRepository
            .findById(id)
            .orElseThrow(() -> noEncontrada());

        Sala sala = salaRepository
            .findById(entrevista.getSalaId())
            .orElseThrow(() -> noEncontrada());

        Producto producto = productoRepository
            .findById(sala.getProductoId())
            .orElseThrow(() -> noEncontrada());

        // No revelar entrevistas de otras empresas.
        if (!empresaId.equals(producto.getEmpresaId())) {
            throw noEncontrada();
        }

        Cuenta representante = cuentaRepository
            .findById(entrevista.getRepresentanteId())
            .orElse(null);

        Grabacion grabacion = grabacionRepository
            .findByEntrevistaId(entrevista.getId())
            .orElse(null);

        Transcripcion transcripcion = grabacion == null
            ? null
            : transcripcionRepository
            .findByGrabacionId(grabacion.getId())
            .orElse(null);

        List<FragmentoTranscripcionDTO> fragmentos =
            List.of();

        EntrevistaAnalisisDetalleDTO analisisDetalle = null;

        if (transcripcion != null) {

            fragmentos = fragmentoRepository
                .findByTranscripcionIdOrderBySegundoInicioAsc(
                    transcripcion.getId())
                .stream()
                .map(f -> new FragmentoTranscripcionDTO(
                    f.getId(),
                    f.getTranscripcionId(),
                    f.getSegundoInicio(),
                    f.getSegundoFin(),
                    f.getTexto()
                ))
                .toList();

            Analisis analisis = analisisRepository
                .findByTranscripcionId(transcripcion.getId())
                .orElse(null);

            if (analisis != null) {

                List<MencionCompetenciaDTO> competidores =
                    mencionRepository
                        .findByAnalisisIdOrderByFrecuenciaDesc(
                            analisis.getId())
                        .stream()
                        .map(m -> new MencionCompetenciaDTO(
                            m.getId(),
                            m.getAnalisisId(),
                            m.getCompetidor(),
                            m.getFrecuencia(),
                            m.getContexto()
                        ))

                        .toList();

                analisisDetalle =
                    new EntrevistaAnalisisDetalleDTO(
                        analisis.getId(),
                        analisis.getEstado(),
                        analisis.getIntencion(),
                        analisis.getSentimiento(),
                        analisis.getFragmentoApoyo(),
                        analisis.getTieneMenciones(),
                        analisis.getMensajeError(),
                        competidores
                    );
            }
        }

        return new EntrevistaDetalleDTO(
            entrevista.getId(),
            entrevista.getSalaId(),
            producto.getId(),
            producto.getNombre(),
            sala.getNombreConsumidor(),
            entrevista.getRepresentanteId(),
            representante == null
                ? null
                : representante.getNombreCompleto(),
            entrevista.getEstado(),
            entrevista.getInicioEn(),
            entrevista.getFinEn(),
            entrevista.getDuracionSegundos(),
            grabacion == null
                ? null : grabacion.getId(),
            transcripcion == null
                ? null : transcripcion.getId(),
            transcripcion == null
                ? null : transcripcion.getEstado(),
            transcripcion == null
                ? null : transcripcion.getTexto(),
            transcripcion == null
                ? null : transcripcion.getIdiomaDetectado(),
            transcripcion == null
                ? null : transcripcion.getProbabilidadIdioma(),
            fragmentos,
            analisisDetalle
        );
    }

    private ResponseStatusException noEncontrada() {
        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Entrevista no encontrada");
    }
}

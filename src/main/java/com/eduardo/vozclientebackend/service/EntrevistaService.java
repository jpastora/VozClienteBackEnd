package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.EntrevistaDTO;
import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.mappers.EntrevistaMapper;
import com.eduardo.vozclientebackend.models.Entrevista;
import com.eduardo.vozclientebackend.repository.EntrevistaRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

import com.eduardo.vozclientebackend.dto.EntrevistaListadoDTO;
import com.eduardo.vozclientebackend.models.*;
import com.eduardo.vozclientebackend.repository.*;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.eduardo.vozclientebackend.enums.EstadoAnalisis;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class EntrevistaService {

    private final EntrevistaRepository entrevistaRepository;
    private final EntrevistaMapper entrevistaMapper;
    private final UsuarioActualService usuarioActualService;

    private final ProductoRepository productoRepository;
    private final GrabacionRepository grabacionRepository;
    private final TranscripcionRepository transcripcionRepository;
    private final AnalisisRepository analisisRepository;

    // Inyectamos los repositorios para las validaciones
    private final SalaRepository salaRepository;
    private final CuentaRepository cuentaRepository; // <-- Nueva inyección

    /**
     * Inicia una nueva entrevista en una sala.
     * Valida que la Sala y la Cuenta (Representante) existan.
     */
    @Transactional
    public EntrevistaDTO iniciarEntrevista(EntrevistaDTO entrevistaDTO) {

        // 1. VALIDACIÓN: Asegurarnos de que la sala existe
        if (entrevistaDTO.salaId() == null || !salaRepository.existsById(entrevistaDTO.salaId())) {
            throw new RuntimeException("No se puede crear la entrevista: La sala indicada no existe.");
        }

        // 2. VALIDACIÓN: Asegurarnos de que el representante (cuenta) existe
        if (entrevistaDTO.representanteId() == null || !cuentaRepository.existsById(entrevistaDTO.representanteId())) {
            throw new RuntimeException("No se puede crear la entrevista: El representante (cuenta) indicado no existe.");
        }

        // 3. Mapeo y lógica de negocio
        Entrevista entrevista = entrevistaMapper.toEntity(entrevistaDTO);

        entrevista.setEstado(EstadoEntrevista.EN_CURSO);
        entrevista.setInicioEn(OffsetDateTime.now());
        entrevista.setFinEn(null);
        entrevista.setDuracionSegundos(null);

        Entrevista entrevistaGuardada = entrevistaRepository.save(entrevista);
        return entrevistaMapper.toDto(entrevistaGuardada);
    }

    @Transactional
    public EntrevistaDTO finalizarEntrevista(Long id) {
        Entrevista entrevista = buscarEntrevistaPorId(id);

        if (entrevista.getEstado() != EstadoEntrevista.EN_CURSO) {
            throw new RuntimeException("Solo se pueden finalizar entrevistas que estén en curso");
        }

        entrevista.setEstado(EstadoEntrevista.FINALIZADA);
        entrevista.setFinEn(OffsetDateTime.now());

        long duracion = ChronoUnit.SECONDS.between(entrevista.getInicioEn(), entrevista.getFinEn());
        entrevista.setDuracionSegundos((int) duracion);

        Entrevista entrevistaActualizada = entrevistaRepository.save(entrevista);
        return entrevistaMapper.toDto(entrevistaActualizada);
    }

    @Transactional
    public EntrevistaDTO reportarFallo(Long id) {
        Entrevista entrevista = buscarEntrevistaPorId(id);

        entrevista.setEstado(EstadoEntrevista.FALLIDA);
        entrevista.setFinEn(OffsetDateTime.now());

        long duracion = ChronoUnit.SECONDS.between(entrevista.getInicioEn(), entrevista.getFinEn());
        entrevista.setDuracionSegundos((int) duracion);

        Entrevista entrevistaActualizada = entrevistaRepository.save(entrevista);
        return entrevistaMapper.toDto(entrevistaActualizada);
    }

    @Transactional
    public void cerrarEntrevistasHuerfanas(int minutosExpiracion) {
        OffsetDateTime limite = OffsetDateTime.now().minusMinutes(minutosExpiracion);

        List<Entrevista> huerfanas = entrevistaRepository.findByEstadoAndInicioEnBefore(
            EstadoEntrevista.EN_CURSO,
            limite
        );

        for (Entrevista entrevista : huerfanas) {
            entrevista.setEstado(EstadoEntrevista.FALLIDA); // O el estado que prefieras para estos casos
            entrevista.setFinEn(OffsetDateTime.now());

            long duracion = ChronoUnit.SECONDS.between(entrevista.getInicioEn(), entrevista.getFinEn());
            entrevista.setDuracionSegundos((int) duracion);
        }

        if (!huerfanas.isEmpty()) {
            entrevistaRepository.saveAll(huerfanas);
        }
    }

    @Transactional(readOnly = true)
    public EntrevistaDTO obtenerPorId(Long id) {
        return entrevistaMapper.toDto(buscarEntrevistaPorId(id));
    }

    @Transactional(readOnly = true)
    public List<EntrevistaDTO> obtenerPorSala(Long salaId) {
        List<Entrevista> entrevistas = entrevistaRepository.findBySalaId(salaId);
        return entrevistaMapper.toDtoList(entrevistas);
    }

    @Transactional(readOnly = true)
    public List<EntrevistaDTO> obtenerPorRepresentante(Long representanteId) {
        List<Entrevista> entrevistas = entrevistaRepository.findByRepresentanteId(representanteId);
        return entrevistaMapper.toDtoList(entrevistas);
    }

    private Entrevista buscarEntrevistaPorId(Long id) {
        return entrevistaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Entrevista no encontrada con el ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<EntrevistaDTO> listarEntrevistas(
        int page,
        Long productoId,
        String estado) {

        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La página no puede ser negativa");
        }

        Long empresaId = usuarioActualService
            .obtenerCuentaActual()
            .getEmpresaId();

        if (empresaId == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El usuario no tiene empresa asignada");
        }

        EstadoEntrevista filtroEstado = null;

        if (estado != null && !estado.isBlank()) {
            try {
                filtroEstado = EstadoEntrevista.valueOf(
                    estado.trim()
                        .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado de entrevista inválido");
            }
        }

        PageRequest pageable = PageRequest.of(
            page,
            10,
            Sort.by(
                Sort.Order.desc("inicioEn"),
                Sort.Order.desc("id")
            )
        );

        return entrevistaRepository.buscarEntrevistas(
            empresaId,
            productoId,
            filtroEstado,
            pageable
        ).map(entrevistaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<EntrevistaListadoDTO> listarEntrevistasDetalladas(
        int page,
        Long productoId,
        String estado) {

        if (page < 0) {
            throw new IllegalArgumentException(
                "La página no puede ser negativa");
        }

        Long empresaId = usuarioActualService
            .obtenerCuentaActual()
            .getEmpresaId();

        if (empresaId == null) {
            throw new IllegalStateException(
                "El usuario no tiene empresa asignada");
        }

        EstadoEntrevista filtroEstado = null;

        if (estado != null && !estado.isBlank()) {
            try {
                filtroEstado = EstadoEntrevista.valueOf(
                    estado.trim().toUpperCase(
                        java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "Estado de entrevista inválido");
            }
        }

        PageRequest pageable = PageRequest.of(
            page, 10,
            Sort.by(
                Sort.Order.desc("inicioEn"),
                Sort.Order.desc("id"))
        );

        Page<Entrevista> pagina =
            entrevistaRepository.buscarEntrevistas(
                empresaId,
                productoId,
                filtroEstado,
                pageable);

        List<Entrevista> entrevistas = pagina.getContent();

        if (entrevistas.isEmpty()) {
            return pagina.map(e -> {
                throw new IllegalStateException(
                    "No debería mapear una página vacía");
            });
        }

        List<Long> salaIds = entrevistas.stream()
            .map(Entrevista::getSalaId)
            .distinct()
            .toList();

        Map<Long, Sala> salas = salaRepository
            .findAllById(salaIds).stream()
            .collect(Collectors.toMap(
                Sala::getId, Function.identity()));

        List<Long> productoIds = salas.values().stream()
            .map(Sala::getProductoId)
            .distinct()
            .toList();

        Map<Long, Producto> productos = productoRepository
            .findAllById(productoIds).stream()
            .collect(Collectors.toMap(
                Producto::getId, Function.identity()));

        List<Long> representanteIds = entrevistas.stream()
            .map(Entrevista::getRepresentanteId)
            .distinct()
            .toList();

        Map<Long, Cuenta> representantes = cuentaRepository
            .findAllById(representanteIds).stream()
            .collect(Collectors.toMap(
                Cuenta::getId, Function.identity()));

        List<Long> entrevistaIds = entrevistas.stream()
            .map(Entrevista::getId)
            .toList();

        Map<Long, Grabacion> grabaciones = grabacionRepository
            .findByEntrevistaIdIn(entrevistaIds)
            .stream()
            .collect(Collectors.toMap(
                Grabacion::getEntrevistaId,
                Function.identity()));

        List<Long> grabacionIds = grabaciones.values()
            .stream()
            .map(Grabacion::getId)
            .toList();

        Map<Long, Transcripcion> transcripciones =
            grabacionIds.isEmpty()
                ? Map.of()
                : transcripcionRepository
                .findByGrabacionIdIn(grabacionIds)
                .stream()
                .collect(Collectors.toMap(
                    Transcripcion::getGrabacionId,
                    Function.identity()));

        List<Long> transcripcionIds = transcripciones
            .values().stream()
            .map(Transcripcion::getId)
            .toList();

        Map<Long, Analisis> analisis =
            transcripcionIds.isEmpty()
                ? Map.of()
                : analisisRepository
                .findByTranscripcionIdIn(
                    transcripcionIds)
                .stream()
                .collect(Collectors.toMap(
                    Analisis::getTranscripcionId,
                    Function.identity()));

        return pagina.map(entrevista -> {
            Sala sala = salas.get(entrevista.getSalaId());

            Producto producto = sala == null
                ? null
                : productos.get(sala.getProductoId());

            Cuenta representante = representantes.get(
                entrevista.getRepresentanteId());

            Grabacion grabacion = grabaciones.get(
                entrevista.getId());

            Transcripcion transcripcion = grabacion == null
                ? null
                : transcripciones.get(grabacion.getId());

            Analisis analisisActual = transcripcion == null
                ? null
                : analisis.get(transcripcion.getId());

            boolean puedeReintentar =
                transcripcion != null
                    && transcripcion.getEstado()
                    == EstadoTranscripcion.CON_ERROR;

            return new EntrevistaListadoDTO(
                entrevista.getId(),
                entrevista.getSalaId(),
                sala == null ? null : sala.getProductoId(),
                producto == null ? null : producto.getNombre(),
                sala == null ? null : sala.getNombreConsumidor(),
                entrevista.getRepresentanteId(),
                representante == null
                    ? null
                    : representante.getNombreCompleto(),
                entrevista.getEstado(),
                entrevista.getInicioEn(),
                entrevista.getFinEn(),
                entrevista.getDuracionSegundos(),
                grabacion == null ? null : grabacion.getId(),
                transcripcion == null
                    ? null : transcripcion.getId(),
                transcripcion == null
                    ? null : transcripcion.getEstado(),
                analisisActual == null
                    ? null : analisisActual.getId(),
                analisisActual == null
                    ? null : analisisActual.getEstado(),
                puedeReintentar
            );
        });
    }

}

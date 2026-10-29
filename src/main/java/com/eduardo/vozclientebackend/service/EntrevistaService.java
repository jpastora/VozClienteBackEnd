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

@Service
@RequiredArgsConstructor
public class EntrevistaService {

    private final EntrevistaRepository entrevistaRepository;
    private final EntrevistaMapper entrevistaMapper;

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
}

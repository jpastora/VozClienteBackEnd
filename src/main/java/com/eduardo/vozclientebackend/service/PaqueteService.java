package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.PaqueteDTO;
import com.eduardo.vozclientebackend.mappers.PaqueteMapper;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaqueteService {

    private final PaqueteRepository paqueteRepository;
    private final PaqueteMapper paqueteMapper;

    /**
     * Crea un nuevo paquete.
     */
    @Transactional
    public PaqueteDTO crearPaquete(PaqueteDTO paqueteDTO) {
        // Validar que el nombre no exista, ya que es unique=true
        Optional<Paquete> existente = paqueteRepository.findByNombre(paqueteDTO.nombre());
        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe un paquete con el nombre: " + paqueteDTO.nombre());
        }

        Paquete paquete = paqueteMapper.toEntity(paqueteDTO);

        OffsetDateTime ahora = OffsetDateTime.now();
        paquete.setCreadoEn(ahora);
        paquete.setActualizadoEn(ahora);

        // Por defecto, si no se especifica, lo creamos como activo
        if (paquete.getEsActivo() == null) {
            paquete.setEsActivo(true);
        }

        Paquete paqueteGuardado = paqueteRepository.save(paquete);
        return paqueteMapper.toDto(paqueteGuardado);
    }

    /**
     * Actualiza un paquete existente.
     */
    @Transactional
    public PaqueteDTO actualizarPaquete(Long id, PaqueteDTO paqueteDTO) {
        Paquete paquete = paqueteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el paquete con ID: " + id));

        // Actualizamos los campos permitidos
        paquete.setNombre(paqueteDTO.nombre());
        paquete.setCantidadSalas(paqueteDTO.cantidadSalas());
        paquete.setPrecio(paqueteDTO.precio());

        if (paqueteDTO.esActivo() != null) {
            paquete.setEsActivo(paqueteDTO.esActivo());
        }

        paquete.setActualizadoEn(OffsetDateTime.now());

        Paquete paqueteActualizado = paqueteRepository.save(paquete);
        return paqueteMapper.toDto(paqueteActualizado);
    }

    /**
     * Obtiene todos los paquetes (activos e inactivos).
     */
    @Transactional(readOnly = true)
    public List<PaqueteDTO> obtenerTodos() {
        List<Paquete> paquetes = paqueteRepository.findAll();
        return paqueteMapper.toDtoList(paquetes);
    }

    /**
     * Obtiene únicamente los paquetes activos (ideal para mostrar en el catálogo).
     */
    @Transactional(readOnly = true)
    public List<PaqueteDTO> obtenerActivos() {
        List<Paquete> paquetes = paqueteRepository.findByEsActivoTrue();
        return paqueteMapper.toDtoList(paquetes);
    }

    /**
     * Obtiene un paquete por su ID.
     */
    @Transactional(readOnly = true)
    public PaqueteDTO obtenerPorId(Long id) {
        Paquete paquete = paqueteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el paquete con ID: " + id));
        return paqueteMapper.toDto(paquete);
    }

    /**
     * Obtiene un paquete por su nombre exacto.
     */
    @Transactional(readOnly = true)
    public PaqueteDTO obtenerPorNombre(String nombre) {
        Paquete paquete = paqueteRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("No se encontró el paquete con nombre: " + nombre));
        return paqueteMapper.toDto(paquete);
    }

    /**
     * Elimina físicamente un paquete (Nota: Puede fallar si ya tiene compras asociadas).
     * Alternativa recomendada: Usar desactivarPaquete()
     */
    @Transactional
    public void eliminarPaquete(Long id) {
        if (!paqueteRepository.existsById(id)) {
            throw new RuntimeException("No se encontró el paquete con ID: " + id);
        }
        paqueteRepository.deleteById(id);
    }

    /**
     * Desactiva un paquete (Soft Delete).
     * Es la mejor opción si el paquete ya tiene compras asociadas para mantener el historial.
     */
    @Transactional
    public void desactivarPaquete(Long id) {
        Paquete paquete = paqueteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el paquete con ID: " + id));

        paquete.setEsActivo(false);
        paquete.setActualizadoEn(OffsetDateTime.now());
        paqueteRepository.save(paquete);
    }
}

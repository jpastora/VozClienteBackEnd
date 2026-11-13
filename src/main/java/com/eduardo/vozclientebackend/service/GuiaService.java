package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.GuiaDTO;
import com.eduardo.vozclientebackend.mappers.GuiaMapper;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.models.Guia;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.GuiaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final EmpresaRepository empresaRepository;
    private final CuentaRepository cuentaRepository;
    private final GuiaMapper guiaMapper;

    /**
     * Crea una nueva guía validando que la empresa y la cuenta existan.
     */
    @Transactional
    public GuiaDTO crearGuia(GuiaDTO dto) {
        validarDatosBasicos(dto);

        Empresa empresa = empresaRepository.findById(dto.empresaId())
            .orElseThrow(() -> new EntityNotFoundException("No se encontró la empresa con ID: " + dto.empresaId()));

        Cuenta cuenta = cuentaRepository.findById(dto.cuentaId())
            .orElseThrow(() -> new EntityNotFoundException("No se encontró la cuenta con ID: " + dto.cuentaId()));

        Guia nuevaGuia = guiaMapper.toEntity(dto);
        nuevaGuia.setEmpresa(empresa);
        nuevaGuia.setCuenta(cuenta);

        Guia guiaGuardada = guiaRepository.save(nuevaGuia);
        return guiaMapper.toDTO(guiaGuardada);
    }

    /**
     * Obtiene una guía por su ID.
     */
    @Transactional(readOnly = true)
    public GuiaDTO obtenerGuiaPorId(Long id) {
        Guia guia = guiaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se encontró la guía con ID: " + id));
        return guiaMapper.toDTO(guia);
    }

    /**
     * Obtiene todas las guías de una empresa en específico.
     */
    @Transactional(readOnly = true)
    public List<GuiaDTO> obtenerGuiasPorEmpresa(Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new EntityNotFoundException("No se encontró la empresa con ID: " + empresaId);
        }
        List<Guia> guias = guiaRepository.findAllByEmpresaId(empresaId);
        return guiaMapper.toDTOs(guias);
    }

    /**
     * Actualiza una guía existente.
     * Solo permite actualizar el título y las preguntas (la cuenta y empresa creadora no deberían cambiar).
     */
    @Transactional
    public GuiaDTO actualizarGuia(Long id, GuiaDTO dto) {
        validarDatosBasicos(dto);

        Guia guiaExistente = guiaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No se encontró la guía con ID: " + id));

        guiaExistente.setTitulo(dto.titulo());

        // Al ser un @ElementCollection, limpiamos y agregamos las nuevas preguntas
        // JPA se encarga de hacer el DELETE y luego los INSERT en la tabla secundaria
        guiaExistente.getPreguntas().clear();
        if (dto.preguntas() != null) {
            guiaExistente.getPreguntas().addAll(dto.preguntas());
        }

        Guia guiaActualizada = guiaRepository.save(guiaExistente);
        return guiaMapper.toDTO(guiaActualizada);
    }

    /**
     * Elimina una guía por su ID.
     */
    @Transactional
    public void eliminarGuia(Long id) {
        if (!guiaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar. No se encontró la guía con ID: " + id);
        }
        guiaRepository.deleteById(id);
    }

    /**
     * Validaciones de negocio reutilizables.
     */
    private void validarDatosBasicos(GuiaDTO dto) {
        if (dto.titulo() == null || dto.titulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título de la guía no puede estar vacío.");
        }
        if (dto.preguntas() == null || dto.preguntas().isEmpty()) {
            throw new IllegalArgumentException("La guía debe contener al menos una pregunta.");
        }
        if (dto.empresaId() == null) {
            throw new IllegalArgumentException("La guía debe estar asociada a una empresa.");
        }
        if (dto.cuentaId() == null) {
            throw new IllegalArgumentException("La guía debe estar asociada a una cuenta.");
        }
    }
}

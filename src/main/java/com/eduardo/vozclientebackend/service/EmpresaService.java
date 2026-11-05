package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.EmpresaDTO;
import com.eduardo.vozclientebackend.mappers.EmpresaMapper;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;

    @Transactional
    public EmpresaDTO crearEmpresa(EmpresaDTO empresaDTO) {
        Empresa empresa = empresaMapper.toEntity(empresaDTO);

        OffsetDateTime ahora = OffsetDateTime.now();
        empresa.setCreadoEn(ahora);
        empresa.setActualizadoEn(ahora);

        Empresa empresaGuardada = empresaRepository.save(empresa);
        return empresaMapper.toDto(empresaGuardada);
    }

    @Transactional
    public EmpresaDTO actualizarEmpresa(Long id, EmpresaDTO empresaDTO) {
        // Utilizamos el método con bloqueo pesimista de tu repositorio
        Empresa empresa = empresaRepository.bloquearEmpresa(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con el ID: " + id));

        // Actualizamos los campos de forma manual ya que el mapper se mantuvo simple
        empresa.setNombre(empresaDTO.nombre());
        empresa.setActualizadoEn(OffsetDateTime.now());

        Empresa empresaActualizada = empresaRepository.save(empresa);
        return empresaMapper.toDto(empresaActualizada);
    }

    @Transactional
    public void eliminarEmpresa(Long id) {
        Empresa empresa = buscarEmpresaPorId(id);
        empresaRepository.delete(empresa);
    }

    @Transactional(readOnly = true)
    public EmpresaDTO obtenerPorId(Long id) {
        return empresaMapper.toDto(buscarEmpresaPorId(id));
    }

    @Transactional(readOnly = true)
    public List<EmpresaDTO> obtenerTodas() {
        List<Empresa> empresas = empresaRepository.findAll();
        return empresaMapper.toDtoList(empresas);
    }

    private Empresa buscarEmpresaPorId(Long id) {
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con el ID: " + id));
    }
}

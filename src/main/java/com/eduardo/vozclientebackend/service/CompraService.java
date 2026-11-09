package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.mappers.CompraMapper;
import com.eduardo.vozclientebackend.models.Compra;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final CompraMapper compraMapper;
    private final PaqueteRepository paqueteRepository;
    private final EmpresaRepository empresaRepository;
    private final CuentaRepository cuentaRepository;

    /**
     * Crea una nueva compra a partir del DTO.
     */
    @Transactional
    public CompraDTO crearCompra(CompraDTO compraDTO) {

        // 1. Validar que la Empresa exista
        if (!empresaRepository.existsById(compraDTO.empresaId())) {
            throw new RuntimeException("No se encontró la empresa con ID: " + compraDTO.empresaId());
        }

        // 2. Validar que la Cuenta exista
        Cuenta cuenta = cuentaRepository.findById(compraDTO.cuentaId())
            .orElseThrow(() -> new RuntimeException("No se encontró la cuenta con ID: " + compraDTO.cuentaId()));

        // 3. Validar seguridad: ¿La cuenta realmente pertenece a esta empresa?
        if (!cuenta.getEmpresaId().equals(compraDTO.empresaId())) {
            throw new RuntimeException("Error de seguridad: La cuenta no pertenece a la empresa indicada.");
        }

        // 4. Validar el Paquete
        Paquete paquete = paqueteRepository.findById(compraDTO.paqueteId())
            .orElseThrow(() -> new RuntimeException("No se encontró el paquete con ID: " + compraDTO.paqueteId()));

        if (!paquete.getEsActivo()) {
            throw new RuntimeException("No se puede comprar un paquete inactivo.");
        }

        // 5. Mapear DTO a Entidad
        Compra compra = compraMapper.toEntity(compraDTO);

        // 6. Asignar los valores reales del paquete (Antifraude)
        compra.setMonto(paquete.getPrecio());
        compra.setSalasAcreditadas(paquete.getCantidadSalas());

        // 7. Valores por defecto
        if (compra.getCreadoEn() == null) {
            compra.setCreadoEn(OffsetDateTime.now());
        }
        if (compra.getClaveIdempotencia() == null) {
            compra.setClaveIdempotencia(UUID.randomUUID());
        }

        // 8. Guardar
        Compra compraGuardada = compraRepository.save(compra);
        return compraMapper.toDto(compraGuardada);
    }

    /**
     * Obtiene todas las compras registradas.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerTodas() {
        List<Compra> compras = compraRepository.findAll();
        return compraMapper.toDtoList(compras);
    }

    /**
     * Obtiene una compra por su ID.
     */
    @Transactional(readOnly = true)
    public CompraDTO obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró la compra con ID: " + id));
        return compraMapper.toDto(compra);
    }

    /**
     * Obtiene la lista de compras asociadas a una Empresa.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorEmpresa(Long empresaId) {
        // Llama al método findByEmpresa definido en tu repositorio
        List<Compra> compras = compraRepository.findByEmpresa(empresaId);
        return compraMapper.toDtoList(compras);
    }

    /**
     * Obtiene la lista de compras asociadas a una Cuenta.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorCuentaId(Long cuentaId) {
        List<Compra> compras = compraRepository.findByCuentaId(cuentaId);
        return compraMapper.toDtoList(compras);
    }

    /**
     * Obtiene la lista de compras asociadas a un Paquete.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorPaquete(Long paqueteId) {
        // Llama al método findByPaquete definido en tu repositorio
        List<Compra> compras = compraRepository.findByPaquete(paqueteId);
        return compraMapper.toDtoList(compras);
    }

    /**
     * Elimina una compra por su ID.
     */
    @Transactional
    public void eliminarCompra(Long id) {
        if (!compraRepository.existsById(id)) {
            throw new RuntimeException("No se encontró la compra con ID: " + id);
        }
        compraRepository.deleteById(id);
    }
}

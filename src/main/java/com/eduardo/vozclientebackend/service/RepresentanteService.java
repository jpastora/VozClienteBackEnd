package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CrearRepresentanteDTO;
import com.eduardo.vozclientebackend.dto.CuentaDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.mappers.CuentaMapper;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RepresentanteService {

    private final CuentaRepository cuentaRepository;
    private final CuentaMapper cuentaMapper;
    private final UsuarioActualService usuarioActualService;

    @Transactional
    public CuentaDTO crearRepresentante(CrearRepresentanteDTO dto) {

        Cuenta administrador = obtenerAdministradorAutorizado();

        String correo = dto.correo()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (cuentaRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada con este correo"
            );
        }

        Cuenta representante = new Cuenta();

        representante.setEmpresaId(administrador.getEmpresaId());
        representante.setNombreCompleto(dto.nombreCompleto().trim());
        representante.setCorreo(correo);
        representante.setRol(RolCuenta.REPRESENTANTE);
        representante.setEstado(EstadoCuenta.PENDIENTE);

        // El representante vinculará Google al iniciar sesión.
        representante.setGoogleSub(null);
        representante.setUltimoIngresoEn(null);

        OffsetDateTime ahora = OffsetDateTime.now();

        representante.setCreadoEn(ahora);
        representante.setActualizadoEn(ahora);

        // La versión inicial se administra con JPA @Version.
        representante.setVersion(null);

        try {
            Cuenta guardada = cuentaRepository.saveAndFlush(representante);
            return cuentaMapper.toDto(guardada);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se pudo crear el representante. Verifique el correo",
                ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<CuentaDTO> listarRepresentantes() {

        Cuenta administrador = obtenerAdministradorAutorizado();

        List<Cuenta> representantes =
            cuentaRepository.findByEmpresaIdAndRolOrderByNombreCompletoAsc(
                administrador.getEmpresaId(),
                RolCuenta.REPRESENTANTE
            );

        return cuentaMapper.toDtoList(representantes);
    }

    @Transactional(readOnly = true)
    public CuentaDTO obtenerRepresentante(Long id) {

        Cuenta administrador = obtenerAdministradorAutorizado();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(
                id,
                administrador.getEmpresaId(),
                RolCuenta.REPRESENTANTE
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        return cuentaMapper.toDto(representante);
    }

    @Transactional
    public CuentaDTO desactivarRepresentante(Long id) {

        Cuenta administrador = obtenerAdministradorAutorizado();

        Cuenta representante = cuentaRepository
            .findByIdAndEmpresaIdAndRol(
                id,
                administrador.getEmpresaId(),
                RolCuenta.REPRESENTANTE
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Representante no encontrado"
            ));

        if (representante.getEstado() == EstadoCuenta.INACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El representante ya está desactivado"
            );
        }

        representante.setEstado(EstadoCuenta.INACTIVA);
        representante.setActualizadoEn(OffsetDateTime.now());

        Cuenta actualizada = cuentaRepository.saveAndFlush(representante);

        return cuentaMapper.toDto(actualizada);
    }

    private Cuenta obtenerAdministradorAutorizado() {

        Cuenta usuario = usuarioActualService.obtenerCuentaActual();

        if (usuario.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no está activa"
            );
        }

        if (usuario.getRol() != RolCuenta.CLIENTE_ADMINISTRADOR) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para administrar representantes"
            );
        }

        if (usuario.getEmpresaId() == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no tiene una empresa asignada"
            );
        }

        return usuario;
    }
}

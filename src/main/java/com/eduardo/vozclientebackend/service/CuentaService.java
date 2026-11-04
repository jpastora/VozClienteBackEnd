package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CambioRolDTO;
import com.eduardo.vozclientebackend.dto.CambiarRolRequestDTO;
import com.eduardo.vozclientebackend.dto.CuentaDTO;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;

import com.eduardo.vozclientebackend.mappers.CuentaMapper;
import com.eduardo.vozclientebackend.models.CambioRol;
import com.eduardo.vozclientebackend.models.Cuenta;

import com.eduardo.vozclientebackend.repository.CambioRolRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final CambioRolRepository cambioRolRepository;
    private final CuentaMapper cuentaMapper;
    private final UsuarioActualService usuarioActualService;
    private final EmpresaRepository empresaRepository;

    /**
     * Lista cuentas dentro del ámbito del administrador.
     */
    @Transactional(readOnly = true)
    public List<CuentaDTO> listarCuentas() {

        Cuenta administrador = obtenerAdministrador();

        List<Cuenta> cuentas;

        if (administrador.getEmpresaId() == null) {

            cuentas = cuentaRepository.findByEmpresaId(null);

        } else {

            cuentas = cuentaRepository.findByEmpresaId(
                administrador.getEmpresaId()
            );
        }

        return cuentaMapper.toDtoList(cuentas);
    }

    /**
     * Cambia el rol de una cuenta y registra el cambio en la bitácora de auditoría.
     */
    @Transactional
    public CuentaDTO cambiarRol(
        Long cuentaId,
        CambiarRolRequestDTO request) {

        Cuenta administrador = obtenerAdministrador();

        if (administrador.getEmpresaId() != null) {

            empresaRepository.bloquearEmpresa(administrador.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Empresa no encontrada"
                ));
        }

        Cuenta cuenta = obtenerCuentaAutorizada(
            cuentaId,
            administrador
        );

        // Validar la versión recibida desde el frontend.
        if (!Objects.equals(
            cuenta.getVersion(),
            request.version())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La cuenta fue modificada. Actualice la información"
            );
        }

        RolCuenta rolAnterior = cuenta.getRol();
        RolCuenta rolNuevo = request.nuevoRol();

        // No permitir cambios innecesarios.
        if (rolAnterior == rolNuevo) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cuenta ya tiene el rol solicitado"
            );
        }

        // Impedir quitar el rol al último administrador.
        if (rolAnterior == RolCuenta.ADMINISTRADOR) {

            long administradoresActivos;

            if (cuenta.getEmpresaId() == null) {

                administradoresActivos =
                    cuentaRepository
                        .countByRolAndEstadoAndEmpresaIdIsNull(
                            RolCuenta.ADMINISTRADOR,
                            EstadoCuenta.ACTIVA
                        );

            } else {

                administradoresActivos =
                    cuentaRepository.countByEmpresaIdAndRolAndEstado(
                        cuenta.getEmpresaId(),
                        RolCuenta.ADMINISTRADOR,
                        EstadoCuenta.ACTIVA
                    );
            }

            if (cuenta.getEstado() == EstadoCuenta.ACTIVA
                && administradoresActivos <= 1) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede quitar el rol al último administrador activo"
                );
            }
        }

        OffsetDateTime ahora = OffsetDateTime.now();

        // Actualizar la cuenta.
        cuenta.setRol(rolNuevo);
        cuenta.setActualizadoEn(ahora);

        Cuenta cuentaActualizada =
            cuentaRepository.saveAndFlush(cuenta);

        // Crear registro de auditoría.
        CambioRol cambio = new CambioRol();

        cambio.setCuentaId(cuentaActualizada.getId());
        cambio.setAutorId(administrador.getId());
        cambio.setRolAnterior(rolAnterior.name());
        cambio.setRolNuevo(rolNuevo.name());
        cambio.setCreadoEn(ahora);

        cambioRolRepository.save(cambio);

        return cuentaMapper.toDto(cuentaActualizada);
    }

    /**
     * Obtiene la bitácora de cambios de una cuenta.
     */
    @Transactional(readOnly = true)
    public List<CambioRolDTO> obtenerHistorial(Long cuentaId) {

        Cuenta administrador = obtenerAdministrador();

        // Validar acceso a la cuenta antes de consultar auditoría.
        obtenerCuentaAutorizada(cuentaId, administrador);

        List<CambioRol> cambios =
            cambioRolRepository.findByCuentaIdOrderByCreadoEnDesc(
                cuentaId
            );

        return cambios.stream()
            .map(cambio -> new CambioRolDTO(
                cambio.getId(),
                cambio.getCuentaId(),
                cambio.getAutorId(),
                cambio.getRolAnterior(),
                cambio.getRolNuevo(),
                cambio.getCreadoEn()
            ))
            .toList();
    }

    /**
     * Obtiene el administrador autenticado.
     */
    private Cuenta obtenerAdministrador() {

        Cuenta usuario = usuarioActualService.obtenerCuentaActual();

        if (usuario.getEstado() != EstadoCuenta.ACTIVA) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no está activa"
            );
        }

        if (usuario.getRol() != RolCuenta.ADMINISTRADOR) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para administrar roles"
            );
        }

        return usuario;
    }

    /**
     * Verifica que la cuenta solicitada pertenezca al mismo ámbito del administrador.
     */
    private Cuenta obtenerCuentaAutorizada(
        Long cuentaId,
        Cuenta administrador) {

        Cuenta cuenta = cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cuenta no encontrada"
            ));

        if (!Objects.equals(
            administrador.getEmpresaId(),
            cuenta.getEmpresaId())) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cuenta no encontrada"
            );
        }

        return cuenta;
    }
}

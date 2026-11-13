package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;

import java.time.OffsetDateTime;

public record CuentaDTO(
        Long id,
        Long empresaId,
        String nombreCompleto,
        String correo,
        String googleSub,
        RolCuenta rol,
        EstadoCuenta estado,
        OffsetDateTime ultimoIngresoEn,
        Integer version,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}

package com.eduardo.vozclientebackend.dto;

import java.time.OffsetDateTime;

public record CambioRolDTO(
        Long id,
        Long cuentaId,
        Long autorId,
        String rolAnterior,
        String rolNuevo,
        OffsetDateTime creadoEn
) {}

package com.eduardo.vozclientebackend.dto;

import java.time.OffsetDateTime;

public record ProductoDTO(
        Long id,
        Long empresaId,
        String nombre,
        String descripcion,
        String categoria,
        Boolean esActivo,
        Integer version,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}

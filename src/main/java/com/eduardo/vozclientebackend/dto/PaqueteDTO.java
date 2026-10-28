package com.eduardo.vozclientebackend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaqueteDTO(
        Long id,
        String nombre,
        Integer cantidadSalas,
        BigDecimal precio,
        Boolean esActivo,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}

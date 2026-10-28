package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoPago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompraDTO(
        Long id,
        Long empresaId,
        Long paqueteId,
        Long cuentaId,
        BigDecimal monto,
        Integer salasAcreditadas,
        EstadoPago estadoPago,
        String referenciaPago,
        UUID claveIdempotencia,
        OffsetDateTime creadoEn
) {}
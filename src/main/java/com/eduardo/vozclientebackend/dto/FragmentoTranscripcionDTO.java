package com.eduardo.vozclientebackend.dto;

import java.math.BigDecimal;

public record FragmentoTranscripcionDTO(
        Long id,
        Long transcripcionId,
        BigDecimal segundoInicio,
        BigDecimal segundoFin,
        String texto
) {}

package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;

import java.time.OffsetDateTime;

public record EntrevistaDTO(
        Long id,
        Long salaId,
        Long representanteId,
        EstadoEntrevista estado,
        OffsetDateTime inicioEn,
        OffsetDateTime finEn,
        Integer duracionSegundos
) {}

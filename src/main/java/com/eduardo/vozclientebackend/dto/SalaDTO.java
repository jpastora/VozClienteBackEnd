package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoSala;

import java.time.OffsetDateTime;

public record SalaDTO(
        Long id,
        Long productoId,
        Long creadoPorId,
        String token,
        EstadoSala estado,
        OffsetDateTime emitidaEn,
        OffsetDateTime venceEn,
        String nombreConsumidor,
        OffsetDateTime aceptoGrabacionEn
) {}

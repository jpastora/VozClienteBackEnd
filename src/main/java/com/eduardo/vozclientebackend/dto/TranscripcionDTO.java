package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TranscripcionDTO(
        Long id,
        Long grabacionId,
        String texto,
        String idiomaDetectado,
        BigDecimal probabilidadIdioma,
        EstadoTranscripcion estado,
        Integer intentos,
        String mensajeError,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}

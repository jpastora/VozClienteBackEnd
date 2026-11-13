package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;

import java.time.OffsetDateTime;

public record EntrevistaListadoDTO(
    Long id,
    Long salaId,
    Long productoId,
    String productoNombre,
    String consumidorNombre,
    Long representanteId,
    String representanteNombre,
    EstadoEntrevista estado,
    OffsetDateTime inicioEn,
    OffsetDateTime finEn,
    Integer duracionSegundos,
    Long grabacionId,
    Long transcripcionId,
    EstadoTranscripcion estadoTranscripcion,
    Long analisisId,
    EstadoAnalisis estadoAnalisis,
    boolean puedeReintentar
) {}

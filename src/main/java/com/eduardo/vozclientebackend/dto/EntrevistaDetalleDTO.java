package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record EntrevistaDetalleDTO(
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
    String textoTranscripcion,
    String idiomaDetectado,
    BigDecimal probabilidadIdioma,
    List<FragmentoTranscripcionDTO> fragmentos,
    EntrevistaAnalisisDetalleDTO analisis
) {}

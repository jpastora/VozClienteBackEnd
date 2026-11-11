package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.enums.SentimientoAnalisis;

import java.util.List;

public record EntrevistaAnalisisDetalleDTO(
    Long id,
    EstadoAnalisis estado,
    IntencionAnalisis intencion,
    SentimientoAnalisis sentimiento,
    String fragmentoApoyo,
    Boolean tieneMenciones,
    String mensajeError,
    List<MencionCompetenciaDTO> competidores
) {}

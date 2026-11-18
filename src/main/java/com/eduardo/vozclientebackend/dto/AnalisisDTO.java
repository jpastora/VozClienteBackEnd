package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.enums.SentimientoAnalisis;

import java.time.OffsetDateTime;
import java.util.List;

public record AnalisisDTO(
        Long id,
        Long transcripcionId,
        EstadoAnalisis estado,
        IntencionAnalisis intencion,
        SentimientoAnalisis sentimiento,
        String fragmentoApoyo,
        Boolean tieneMenciones,
        List<MencionCompetenciaDTO> menciones,
        String proveedorIa,
        Short intentos,
        String mensajeError,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}



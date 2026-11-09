package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoAnalisis;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.enums.SentimientoAnalisis;

import java.time.OffsetDateTime;

public record AnalisisDTO(
        Long id,
        Long transcripcionId,
        EstadoAnalisis estado,
        IntencionAnalisis intencion,
        SentimientoAnalisis sentimiento,
        String fragmentoApoyo,
        Boolean tieneMenciones,
        String proveedorIa,
        Short intentos,
        String mensajeError,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}



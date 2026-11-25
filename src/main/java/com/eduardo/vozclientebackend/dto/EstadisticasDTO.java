package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.IntencionAnalisis;

import java.util.List;

public record EstadisticasDTO(

    long entrevistasAnalizadas,

    double intencionCompra,

    long mencionesCompetencia,

    List<DistribucionIntencionDTO> distribucionIntencion,

    List<CompetidorDTO> competidores

) {

    public record DistribucionIntencionDTO(

        IntencionAnalisis intencion,

        long cantidad,

        double porcentaje

    ) {}

    public record CompetidorDTO(

        String competidor,

        long menciones

    ) {}
}

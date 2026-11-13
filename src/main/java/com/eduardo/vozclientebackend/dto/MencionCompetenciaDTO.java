package com.eduardo.vozclientebackend.dto;

public record MencionCompetenciaDTO(
        Long id,
        Long analisisId,
        String competidor,
        Short frecuencia,
        String contexto
) {}

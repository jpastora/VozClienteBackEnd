package com.eduardo.vozclientebackend.dto;

public record FinalizarGrabacionDTO(
    Integer duracionSegundos,
    Boolean tieneAudioCliente,
    Boolean esParcial
) {}

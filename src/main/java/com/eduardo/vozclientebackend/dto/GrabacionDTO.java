package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoGrabacion;

import java.time.OffsetDateTime;

public record GrabacionDTO(
        Long id,
        Long entrevistaId,
        String rutaCanalCliente,
        String rutaCanalRepresentante,
        String formato,
        Long tamanoBytes,
        Integer duracionSegundos,
        String hashSha256,
        Boolean tieneAudioCliente,
        Boolean esParcial,
        EstadoGrabacion estado,
        OffsetDateTime creadoEn
) {}

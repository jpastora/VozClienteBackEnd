package com.eduardo.vozclientebackend.dto;


import java.time.OffsetDateTime;


public record EmpresaDTO(
        Long id,
        String nombre,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {}

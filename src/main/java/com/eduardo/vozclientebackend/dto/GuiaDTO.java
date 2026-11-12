package com.eduardo.vozclientebackend.dto;

import java.util.List;

public record GuiaDTO(
    Long id,
    String titulo,
    List<String> preguntas,
    Long empresaId,
    Long cuentaId
) {}

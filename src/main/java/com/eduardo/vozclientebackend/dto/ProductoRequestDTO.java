
package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductoRequestDTO(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(
        min = 3,
        max = 100,
        message = "El nombre debe tener entre 3 y 100 caracteres"
    )
    String nombre,

    @Size(
        max = 500,
        message = "La descripción no puede exceder los 500 caracteres"
    )
    String descripcion,

    @Size(
        max = 60,
        message = "La categoría no puede exceder los 60 caracteres"
    )
    String categoria,

    Boolean esActivo

) {}

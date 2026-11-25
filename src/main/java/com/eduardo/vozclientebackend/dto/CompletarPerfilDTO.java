package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario obligatorio del primer ingreso con Google (POST /api/auth/completar-perfil).
 *
 * @param nombreCompleto nombre de la persona (2 a 100 caracteres)
 * @param nombreEmpresa  nombre de su empresa (2 a 150 caracteres)
 */
public record CompletarPerfilDTO(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = ReglasNombre.PATRON, message = ReglasNombre.MENSAJE)
    String nombreCompleto,

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre de la empresa debe tener entre 2 y 150 caracteres")
    String nombreEmpresa

) {
}

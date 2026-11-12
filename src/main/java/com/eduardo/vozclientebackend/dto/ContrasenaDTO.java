package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contraseña que el cliente-administrador asigna a un representante
 * (PATCH /api/representantes/{id}/contrasena).
 *
 * @param contrasena contraseña en texto plano; se guarda solo su hash BCrypt
 */
public record ContrasenaDTO(

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    String contrasena

) {
}

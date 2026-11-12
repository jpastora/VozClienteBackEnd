package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para ingresar con correo y contraseña (POST /api/auth/ingresar).
 *
 * @param correo     correo de la cuenta
 * @param contrasena contraseña en texto plano; viaja solo por la solicitud y nunca se guarda así
 */
public record IngresoDTO(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede superar 72 caracteres")
    String contrasena

) {
}

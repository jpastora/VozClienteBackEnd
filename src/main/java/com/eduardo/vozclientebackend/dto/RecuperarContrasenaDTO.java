package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de recuperación de contraseña ("¿Olvidó su contraseña?").
 *
 * @param correo correo de la cuenta
 */
public record RecuperarContrasenaDTO(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo

) {
}

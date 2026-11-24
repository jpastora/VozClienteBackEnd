package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edición de los datos de una cuenta por el administrador de la plataforma (PUT /api/cuentas/{id}).
 *
 * @param nombreCompleto nombre de la persona (2 a 100 caracteres)
 * @param correo         correo de la cuenta; no se puede cambiar si la cuenta ingresa con Google
 * @param empresaId      empresa de la cuenta; se ignora en un ADMINISTRADOR, que no pertenece a ninguna
 * @param version        versión de la cuenta que vio quien edita
 */
public record EditarCuentaDTO(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = ReglasNombre.PATRON, message = ReglasNombre.MENSAJE)
    String nombreCompleto,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo,

    Long empresaId,

    @NotNull(message = "La versión de la cuenta es obligatoria")
    Integer version

) {
}

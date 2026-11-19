package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.RolCuenta;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una cuenta por el administrador de la plataforma.
 *
 * @param nombreCompleto nombre de la persona
 * @param correo         correo; recibe la contraseña temporal
 * @param rol            rol de la cuenta
 * @param empresaId      empresa de la cuenta; obligatoria salvo para el rol ADMINISTRADOR (se ignora)
 */
public record CrearCuentaDTO(

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombreCompleto,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo,

    @NotNull(message = "El rol es obligatorio")
    RolCuenta rol,

    Long empresaId,

    /** Verdadero si la persona ingresará con Google: no se genera contraseña ni se envía correo. */
    Boolean ingresaConGoogle

) {
    public CrearCuentaDTO(String nombreCompleto, String correo, RolCuenta rol, Long empresaId) {
        this(nombreCompleto, correo, rol, empresaId, false);
    }
}

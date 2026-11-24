package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cuenta con correo y contraseña (POST /api/auth/registro).
 * Crea la empresa y la cuenta como cliente-administrador, igual que el primer ingreso con Google.
 *
 * @param nombreCompleto nombre de la persona (2 a 100 caracteres)
 * @param correo         correo de la cuenta; único en la plataforma
 * @param contrasena     contraseña en texto plano; se guarda solo su hash BCrypt
 * @param nombreEmpresa  nombre de la empresa que se crea (2 a 150 caracteres)
 */
public record RegistroDTO(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = ReglasNombre.PATRON, message = ReglasNombre.MENSAJE)
    String nombreCompleto,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 20, message = "La contraseña debe tener entre 8 y 20 caracteres")
    String contrasena,

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre de la empresa debe tener entre 2 y 150 caracteres")
    String nombreEmpresa

) {

    /** Sin nombre de empresa: la empresa se llama "Empresa de" más el nombre de la persona. */
    public RegistroDTO(String nombreCompleto, String correo, String contrasena) {
        this(nombreCompleto, correo, contrasena, null);
    }
}

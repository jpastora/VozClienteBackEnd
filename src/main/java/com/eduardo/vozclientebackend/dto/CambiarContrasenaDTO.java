package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contraseña de la persona con sesión iniciada (obligatorio con contraseña temporal).
 *
 * @param contrasenaActual contraseña vigente (la temporal, la primera vez)
 * @param contrasenaNueva  contraseña nueva; debe cumplir la política de contraseñas
 */
public record CambiarContrasenaDTO(

    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 72, message = "La contraseña actual no es válida")
    String contrasenaActual,

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(max = 72, message = "La contraseña nueva no es válida")
    String contrasenaNueva

) {}

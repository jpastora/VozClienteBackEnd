package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarRepresentanteDTO(

    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombreCompleto,

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo,

    @NotNull(message = "La versión de la cuenta es obligatoria")
    Integer version

) {
    @AssertTrue(message = "Debe enviar al menos el nombre completo o el correo electrónico")
    public boolean tieneAlMenosUnCampoValido() {
        boolean tieneNombre = nombreCompleto != null && !nombreCompleto.isBlank();
        boolean tieneCorreo = correo != null && !correo.isBlank();
        return tieneNombre || tieneCorreo;
    }
}

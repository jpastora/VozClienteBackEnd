package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRepresentanteDTO(

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombreCompleto,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres")
    String correo

) {}

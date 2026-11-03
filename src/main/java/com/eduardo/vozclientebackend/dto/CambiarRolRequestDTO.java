package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.RolCuenta;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequestDTO(

    @NotNull(message = "El nuevo rol es obligatorio")
    RolCuenta nuevoRol,

    @NotNull(message = "La versión de la cuenta es obligatoria")
    Integer version

) {}

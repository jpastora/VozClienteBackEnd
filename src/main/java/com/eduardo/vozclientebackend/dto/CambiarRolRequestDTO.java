package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.RolCuenta;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de rol de una cuenta.
 *
 * @param nuevoRol  rol nuevo
 * @param version   versión de la cuenta que vio quien hace el cambio
 * @param empresaId empresa que queda asignada; obligatoria al pasar un ADMINISTRADOR a otro rol, y se
 *                  ignora en los demás casos (al promover a ADMINISTRADOR la empresa se quita)
 */
public record CambiarRolRequestDTO(

    @NotNull(message = "El nuevo rol es obligatorio")
    RolCuenta nuevoRol,

    @NotNull(message = "La versión de la cuenta es obligatoria")
    Integer version,

    Long empresaId

) {}

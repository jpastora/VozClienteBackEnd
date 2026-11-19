package com.eduardo.vozclientebackend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Activar o desactivar una cuenta.
 *
 * @param activa  {@code true} para reactivar, {@code false} para desactivar
 * @param version versión de la cuenta que vio quien hace el cambio (control de concurrencia)
 */
public record CambiarEstadoRequestDTO(

    @NotNull(message = "Indica si la cuenta queda activa o inactiva")
    Boolean activa,

    @NotNull(message = "La versión de la cuenta es obligatoria")
    Integer version

) {}

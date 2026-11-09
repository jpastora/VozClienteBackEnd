package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.RolCuenta;

/**
 * Datos de la persona con sesión iniciada, para que el front-end arme la navegación por rol (HU-01).
 *
 * @param id             identificador de la cuenta
 * @param nombreCompleto nombre de la persona
 * @param correo         correo de Google
 * @param rol            rol vigente de la cuenta
 * @param empresaId      empresa de la cuenta; nulo para el administrador de la plataforma
 */
public record SesionDTO(
    Long id,
    String nombreCompleto,
    String correo,
    RolCuenta rol,
    Long empresaId
) {
}

package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;

import java.time.OffsetDateTime;

/**
 * Cuenta tal como la ve el administrador de la plataforma en el módulo de usuarios.
 *
 * @param empresaNombre            nombre de la empresa; nulo para el administrador de la plataforma
 * @param contrasenaTemporalPendiente verdadero si todavía no cambió la contraseña temporal
 * @param ingresaConGoogle         verdadero si la cuenta ya está vinculada a Google (no usa contraseña)
 * @param correoEnviado            solo al crear o reenviar: si el servidor de correo aceptó el mensaje;
 *                                 nulo en el resto de las respuestas
 */
public record CuentaGlobalDTO(
    Long id,
    Long empresaId,
    String empresaNombre,
    String nombreCompleto,
    String correo,
    RolCuenta rol,
    EstadoCuenta estado,
    OffsetDateTime ultimoIngresoEn,
    Integer version,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn,
    boolean contrasenaTemporalPendiente,
    boolean ingresaConGoogle,
    Boolean correoEnviado
) {}

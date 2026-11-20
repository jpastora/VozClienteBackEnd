package com.eduardo.vozclientebackend.dto;

/**
 * Saldo de salas de una empresa (RF-17, RF-18). No se guarda en la base de datos: se calcula a
 * partir de las compras y de las salas (ERS, decisiones de modelado: atributo derivado).
 *
 * @param empresaId   empresa consultada
 * @param acreditadas salas de todas las compras con pago aprobado
 * @param consumidas  salas cuya llamada terminó con una grabación correcta (ya descontadas)
 * @param reservadas  enlaces vigentes que todavía no se usan; cuentan para no emitir más enlaces
 *                    de los que el saldo permite
 * @param saldo       acreditadas menos consumidas; nunca es negativo
 * @param disponibles salas que todavía se pueden usar para generar enlaces nuevos
 */
public record SaldoSalasDTO(
    Long empresaId,
    long acreditadas,
    long consumidas,
    long reservadas,
    long saldo,
    long disponibles
) {
}

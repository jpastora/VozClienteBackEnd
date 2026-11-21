package com.eduardo.vozclientebackend.dto;

import java.util.UUID;

/**
 * Compra de un paquete con pago simulado (RF-18). La empresa y la cuenta salen de la sesión, y el
 * monto, las salas y el estado del pago los decide el back-end: nada de eso se acepta del cliente.
 *
 * <p>Los datos de la tarjeta de prueba solo se validan; nunca se guardan ni se registran en los
 * logs. De la tarjeta solo se conservan los últimos 4 dígitos en la referencia del pago.</p>
 *
 * @param paqueteId         paquete elegido
 * @param claveIdempotencia identificador único del intento; si el formulario se envía dos veces
 *                          con la misma clave, se registra una sola compra
 * @param titular           nombre del titular de la tarjeta de prueba
 * @param numeroTarjeta     16 dígitos (se admiten espacios o guiones)
 * @param vencimiento       mes y año en formato MM/AA
 * @param cvv               3 dígitos
 */
public record PagoPaqueteDTO(
    Long paqueteId,
    UUID claveIdempotencia,
    String titular,
    String numeroTarjeta,
    String vencimiento,
    String cvv
) {
}

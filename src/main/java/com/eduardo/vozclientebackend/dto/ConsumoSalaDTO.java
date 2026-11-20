package com.eduardo.vozclientebackend.dto;

import java.time.OffsetDateTime;

/**
 * Una sala descontada del saldo: su llamada terminó con una grabación correcta (RF-18).
 *
 * @param salaId           sala consumida
 * @param productoId       producto entrevistado
 * @param producto         nombre del producto
 * @param nombreConsumidor nombre que indicó el consumidor
 * @param consumidaEn      fecha en que se guardó la grabación correcta
 */
public record ConsumoSalaDTO(
    Long salaId,
    Long productoId,
    String producto,
    String nombreConsumidor,
    OffsetDateTime consumidaEn
) {
}

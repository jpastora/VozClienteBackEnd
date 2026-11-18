package com.eduardo.vozclientebackend.dto;

import com.eduardo.vozclientebackend.enums.CanalGrabacion;

/**
 * Confirmación de un fragmento. El front end usa ultimaSecuencia para saber desde dónde
 * reanudar el envío después de una caída de red.
 */
public record FragmentoAckDTO(
    Long entrevistaId,
    CanalGrabacion canal,
    Integer ultimaSecuencia,
    Long bytesConfirmados
) {}

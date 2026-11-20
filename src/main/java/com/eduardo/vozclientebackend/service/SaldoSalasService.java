package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ConsumoSalaDTO;
import com.eduardo.vozclientebackend.dto.SaldoSalasDTO;
import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.enums.EstadoPago;
import com.eduardo.vozclientebackend.enums.EstadoSala;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Saldo de salas de cada empresa (RF-07, RF-17 y RF-18).
 *
 * <p>El saldo no se guarda: se calcula cada vez a partir de los datos, como define el modelo
 * conceptual del ERS. Así nunca queda desfasado y no hay que "devolver" salas a mano.</p>
 *
 * <ul>
 *   <li><b>Acreditadas:</b> salas de las compras con pago {@link EstadoPago#APROBADO}.</li>
 *   <li><b>Consumidas:</b> se descuenta una sala cuando la llamada termina con una grabación
 *       correcta. Si la grabación falla o queda sin contenido, no se descuenta.</li>
 *   <li><b>Reservadas:</b> enlaces vigentes que todavía no se usan. No se descuentan, pero impiden
 *       emitir más enlaces de los que el saldo cubre. Si el enlace vence o se elimina, la reserva se
 *       libera sola.</li>
 *   <li><b>Saldo</b> = acreditadas − consumidas, y <b>disponibles</b> = saldo − reservadas. Ninguno
 *       de los dos es negativo.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SaldoSalasService {

    /** Mensaje cuando no se puede generar un enlace por falta de saldo (RF-07). */
    public static final String SIN_SALDO =
        "Tu empresa no tiene salas disponibles. Compra un paquete en «Paquetes y saldo» para generar más enlaces.";

    /** Estados de grabación que cuentan como llamada consumida (grabación correcta). */
    static final Set<EstadoGrabacion> GRABACION_CORRECTA = EnumSet.of(EstadoGrabacion.COMPLETADA);

    private final CompraRepository compraRepository;
    private final SalaRepository salaRepository;
    private final EmpresaRepository empresaRepository;
    private final Clock reloj;

    /**
     * Saldo actual de la empresa.
     *
     * @param empresaId empresa a consultar
     * @return acreditadas, consumidas, reservadas, saldo y disponibles
     */
    @Transactional(readOnly = true)
    public SaldoSalasDTO obtener(Long empresaId) {
        return calcular(empresaId);
    }

    /**
     * Comprueba, antes de emitir un enlace, que la empresa tenga al menos una sala disponible.
     * Bloquea la fila de la empresa hasta que termine la transacción de quien llama, para que dos
     * enlaces generados al mismo tiempo no usen la misma sala.
     *
     * @param empresaId empresa dueña del producto
     * @throws ResponseStatusException 409 si no hay salas disponibles; 404 si la empresa no existe
     */
    @Transactional
    public void exigirSalaDisponible(Long empresaId) {
        empresaRepository.bloquearEmpresa(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no existe"));

        if (calcular(empresaId).disponibles() < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, SIN_SALDO);
        }
    }

    /**
     * Historial de consumo: las salas que ya se descontaron, la más reciente primero.
     *
     * @param empresaId empresa a consultar
     * @return una fila por sala consumida
     */
    @Transactional(readOnly = true)
    public List<ConsumoSalaDTO> consumos(Long empresaId) {
        return salaRepository.listarConsumosPorEmpresa(empresaId, GRABACION_CORRECTA);
    }

    /** Calcula el saldo con los datos actuales. */
    SaldoSalasDTO calcular(Long empresaId) {
        Long sumaAcreditadas = compraRepository.sumarSalasPorEstado(empresaId, EstadoPago.APROBADO);
        long acreditadas = sumaAcreditadas == null ? 0 : sumaAcreditadas;
        long consumidas = salaRepository.contarConsumidasPorEmpresa(empresaId, GRABACION_CORRECTA);
        long reservadas = salaRepository.contarReservadasPorEmpresa(
            empresaId, OffsetDateTime.now(reloj), EstadoSala.EXPIRADO, GRABACION_CORRECTA);

        long saldo = Math.max(0, acreditadas - consumidas);
        long disponibles = Math.max(0, saldo - reservadas);
        return new SaldoSalasDTO(empresaId, acreditadas, consumidas, reservadas, saldo, disponibles);
    }
}

package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.dto.PagoPaqueteDTO;
import com.eduardo.vozclientebackend.dto.SaldoSalasDTO;
import com.eduardo.vozclientebackend.enums.EstadoPago;
import com.eduardo.vozclientebackend.mappers.CompraMapper;
import com.eduardo.vozclientebackend.models.Compra;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.PaqueteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Compras de paquetes con pago simulado (RF-18).
 *
 * <ul>
 *   <li>La empresa y la cuenta son las de la sesión; el monto y las salas salen del paquete; el
 *       estado del pago lo decide la pasarela simulada. Nada de eso se acepta del cliente.</li>
 *   <li>Solo se compran paquetes activos.</li>
 *   <li>Si el pago se rechaza, la compra queda registrada como RECHAZADO y no acredita salas.</li>
 *   <li>Si el formulario se envía dos veces con la misma clave de idempotencia, se devuelve la compra
 *       ya registrada: nunca se cobra dos veces.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final CuentaRepository cuentaRepository;
    private final EmpresaRepository empresaRepository;
    private final PaqueteRepository paqueteRepository;
    private final CompraMapper compraMapper;
    private final PagoSimuladoService pagoSimulado;
    private final SaldoSalasService saldoSalasService;
    private final Clock reloj;

    /**
     * Compra un paquete para la empresa de la cuenta con pago simulado.
     *
     * @param datos  paquete, clave de idempotencia y datos de la tarjeta de prueba
     * @param cuenta cuenta con sesión (cliente-administrador)
     * @return la compra registrada, aprobada o rechazada
     * @throws ResponseStatusException 400 si faltan datos o la tarjeta no es válida; 403 si la
     *                                 cuenta no tiene empresa; 404 si el paquete no existe; 409 si el
     *                                 paquete está inactivo o la clave ya la usó otra empresa
     */
    @Transactional
    public CompraDTO comprarPaquete(PagoPaqueteDTO datos, Cuenta cuenta) {
        if (datos == null || datos.paqueteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige el paquete que quieres comprar.");
        }
        if (datos.claveIdempotencia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la clave de la compra. Recarga la página.");
        }
        Long empresaId = cuenta.getEmpresaId();
        if (empresaId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo una cuenta de empresa puede comprar paquetes.");
        }

        // Se bloquea la empresa: dos compras simultáneas de la misma empresa se atienden en orden y
        // la revisión de la clave de idempotencia no tiene carreras.
        Empresa empresa = empresaRepository.bloquearEmpresa(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no existe"));

        Optional<Compra> previa = compraRepository.findByClaveIdempotencia(datos.claveIdempotencia());
        if (previa.isPresent()) {
            if (!empresaId.equals(previa.get().getEmpresa().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta compra ya fue registrada. Recarga la página.");
            }
            return compraMapper.toDto(previa.get());
        }

        Paquete paquete = paqueteRepository.findById(datos.paqueteId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El paquete no existe."));
        if (!Boolean.TRUE.equals(paquete.getEsActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Este paquete ya no está disponible. Elige otro del catálogo.");
        }

        PagoSimuladoService.Resultado pago = pagoSimulado.procesar(
            datos.titular(), datos.numeroTarjeta(), datos.vencimiento(), datos.cvv(), datos.claveIdempotencia());

        Compra compra = new Compra();
        compra.setEmpresa(empresa);
        compra.setPaquete(paquete);
        compra.setCuenta(cuentaRepository.getReferenceById(cuenta.getId()));
        // Se copian el precio y las salas del paquete: editarlo después no altera esta compra (RF-17).
        compra.setMonto(paquete.getPrecio());
        compra.setSalasAcreditadas(paquete.getCantidadSalas());
        compra.setEstadoPago(pago.aprobado() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO);
        compra.setReferenciaPago(pago.referencia());
        compra.setClaveIdempotencia(datos.claveIdempotencia());
        compra.setCreadoEn(OffsetDateTime.now(reloj));

        return compraMapper.toDto(compraRepository.save(compra));
    }

    /**
     * Obtiene todas las compras registradas.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerTodas() {
        return compraMapper.toDtoList(compraRepository.findAll());
    }

    /**
     * Obtiene una compra por su ID.
     */
    @Transactional(readOnly = true)
    public CompraDTO obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la compra"));
        return compraMapper.toDto(compra);
    }

    /**
     * Historial de compras de una empresa, la más reciente primero.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorEmpresa(Long empresaId) {
        return compraMapper.toDtoList(compraRepository.findByEmpresaIdOrderByCreadoEnDesc(empresaId));
    }

    /**
     * Obtiene la lista de compras asociadas a una Cuenta.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorCuentaId(Long cuentaId) {
        return compraMapper.toDtoList(compraRepository.findByCuentaId(cuentaId));
    }

    /**
     * Obtiene la lista de compras asociadas a un Paquete.
     */
    @Transactional(readOnly = true)
    public List<CompraDTO> obtenerPorPaquete(Long paqueteId) {
        return compraMapper.toDtoList(compraRepository.findByPaqueteId(paqueteId));
    }

    /**
     * Elimina una compra (solo el administrador de la plataforma). Una compra aprobada no se puede
     * eliminar si la empresa ya usó o reservó esas salas, porque el saldo quedaría negativo (RNF-09).
     *
     * @throws ResponseStatusException 404 si no existe; 409 si dejaría el saldo negativo
     */
    @Transactional
    public void eliminarCompra(Long id) {
        Compra compra = compraRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la compra"));

        if (compra.getEstadoPago() == EstadoPago.APROBADO) {
            Long empresaId = compra.getEmpresa().getId();
            empresaRepository.bloquearEmpresa(empresaId);
            SaldoSalasDTO saldo = saldoSalasService.obtener(empresaId);
            long acreditadasSinEsta = saldo.acreditadas() - compra.getSalasAcreditadas();
            if (acreditadasSinEsta < saldo.consumidas() + saldo.reservadas()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la empresa ya usó o reservó salas de esta compra.");
            }
        }
        compraRepository.delete(compra);
    }
}

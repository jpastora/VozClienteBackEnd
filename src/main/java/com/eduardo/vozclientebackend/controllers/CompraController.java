package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.security.AccesoEmpresa;
import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.dto.PagoPaqueteDTO;
import com.eduardo.vozclientebackend.service.CompraService;
import com.eduardo.vozclientebackend.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final AccesoEmpresa acceso;

    private final CompraService compraService;
    private final UsuarioActualService usuarioActualService;

    /**
     * Compra un paquete con pago simulado para la empresa de la sesión (RF-18).
     * Endpoint: POST /api/compras
     *
     * <p>201 con la compra aprobada o rechazada (el estado viene en {@code estadoPago}); 400 si la
     * tarjeta de prueba no es válida; 409 si el paquete está inactivo.</p>
     */
    @PreAuthorize("hasAuthority('compra:crear')")
    @PostMapping
    public ResponseEntity<CompraDTO> crearCompra(@RequestBody PagoPaqueteDTO pago) {
        CompraDTO compra = compraService.comprarPaquete(pago, usuarioActualService.obtenerCuentaActual());
        return new ResponseEntity<>(compra, HttpStatus.CREATED);
    }

    /**
     * Obtiene todas las compras.
     * Endpoint: GET /api/compras
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping
    public ResponseEntity<List<CompraDTO>> obtenerTodasLasCompras() {
        List<CompraDTO> compras = acceso.filtrar(compraService.obtenerTodas(), CompraDTO::empresaId);
        return ResponseEntity.ok(compras);
    }

    /**
     * Obtiene una compra específica por su ID.
     * Endpoint: GET /api/compras/{id}
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<CompraDTO> obtenerCompraPorId(@PathVariable Long id) {
        CompraDTO compra = compraService.obtenerPorId(id);
        acceso.exigirEmpresa(compra.empresaId());
        return ResponseEntity.ok(compra);
    }

    /**
     * Obtiene todas las compras asociadas a una empresa.
     * Endpoint: GET /api/compras/empresa/{empresaId}
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<CompraDTO>> obtenerComprasPorEmpresa(@PathVariable Long empresaId) {
        acceso.exigirEmpresa(empresaId);
        List<CompraDTO> compras = compraService.obtenerPorEmpresa(empresaId);
        return ResponseEntity.ok(compras);
    }

    /**
     * Obtiene todas las compras asociadas a una cuenta.
     * Endpoint: GET /api/compras/cuenta/{cuentaId}
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/cuenta/{cuentaId}")
    public ResponseEntity<List<CompraDTO>> obtenerComprasPorCuenta(@PathVariable Long cuentaId) {
        acceso.exigirCuenta(cuentaId);
        List<CompraDTO> compras = compraService.obtenerPorCuentaId(cuentaId);
        return ResponseEntity.ok(compras);
    }

    /**
     * Obtiene todas las compras asociadas a un paquete.
     * Endpoint: GET /api/compras/paquete/{paqueteId}
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/paquete/{paqueteId}")
    public ResponseEntity<List<CompraDTO>> obtenerComprasPorPaquete(@PathVariable Long paqueteId) {
        List<CompraDTO> compras = acceso.filtrar(compraService.obtenerPorPaquete(paqueteId), CompraDTO::empresaId);
        return ResponseEntity.ok(compras);
    }

    /**
     * Elimina una compra por su ID.
     * Endpoint: DELETE /api/compras/{id}
     */
    @PreAuthorize("hasAuthority('compra:gestionar')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCompra(@PathVariable Long id) {
        compraService.eliminarCompra(id);
        return ResponseEntity.noContent().build();
    }
}

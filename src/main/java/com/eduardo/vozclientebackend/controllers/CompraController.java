package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.service.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    /**
     * Crea una nueva compra.
     * Endpoint: POST /api/compras
     */
    @PreAuthorize("hasAuthority('compra:crear')")
    @PostMapping
    public ResponseEntity<CompraDTO> crearCompra(@RequestBody CompraDTO compraDTO) {
        CompraDTO nuevaCompra = compraService.crearCompra(compraDTO);
        return new ResponseEntity<>(nuevaCompra, HttpStatus.CREATED);
    }

    /**
     * Obtiene todas las compras.
     * Endpoint: GET /api/compras
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping
    public ResponseEntity<List<CompraDTO>> obtenerTodasLasCompras() {
        List<CompraDTO> compras = compraService.obtenerTodas();
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
        return ResponseEntity.ok(compra);
    }

    /**
     * Obtiene todas las compras asociadas a una empresa.
     * Endpoint: GET /api/compras/empresa/{empresaId}
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<CompraDTO>> obtenerComprasPorEmpresa(@PathVariable Long empresaId) {
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
        List<CompraDTO> compras = compraService.obtenerPorPaquete(paqueteId);
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

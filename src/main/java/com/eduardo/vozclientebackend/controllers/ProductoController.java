
package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.service.ProductoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(
        @Valid @RequestBody ProductoRequestDTO request) {

        ProductoDTO creado = productoService.crearProducto(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(creado);
    }

    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping
    public ResponseEntity<Page<ProductoDTO>> listarProductos(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String nombre,
        @RequestParam(required = false) String estado) {

        if (page < 0) {
            throw new IllegalArgumentException(
                "El número de página no puede ser negativo."
            );
        }

        return ResponseEntity.ok(
            productoService.listarProductos(page, nombre, estado)
        );
    }

    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping("/activos")
    public ResponseEntity<List<ProductoDTO>> obtenerActivos() {
        return ResponseEntity.ok(
            productoService.obtenerActivosPorEmpresa()
        );
    }

    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerPorId(
        @PathVariable Long id) {

        return ResponseEntity.ok(
            productoService.obtenerPorId(id)
        );
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
        @PathVariable Long id,
        @Valid @RequestBody ProductoRequestDTO request) {

        return ResponseEntity.ok(
            productoService.actualizarProducto(id, request)
        );
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarProducto(
        @PathVariable Long id) {

        productoService.desactivarProducto(id);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
        @PathVariable Long id) {

        productoService.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}

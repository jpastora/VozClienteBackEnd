package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO request) {
        ProductoDTO nuevoProducto = productoService.crearProducto(request);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizarProducto(
        @PathVariable Long id,
        @Valid @RequestBody ProductoRequestDTO request) {
        ProductoDTO productoActualizado = productoService.actualizarProducto(id, request);
        return ResponseEntity.ok(productoActualizado);
    }

    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerPorId(@PathVariable Long id) {
        ProductoDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    // Ruta simplificada: GET /api/productos
    // Devuelve automáticamente los productos de la empresa del usuario
    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping
    public ResponseEntity<List<ProductoDTO>> obtenerTodos() {
        List<ProductoDTO> productos = productoService.obtenerTodosPorEmpresa();
        return ResponseEntity.ok(productos);
    }

    // Ruta simplificada: GET /api/productos/activos
    @PreAuthorize("hasAuthority('producto:leer')")
    @GetMapping("/activos")
    public ResponseEntity<List<ProductoDTO>> obtenerActivos() {
        List<ProductoDTO> productos = productoService.obtenerActivosPorEmpresa();
        return ResponseEntity.ok(productos);
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('producto:escribir')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}

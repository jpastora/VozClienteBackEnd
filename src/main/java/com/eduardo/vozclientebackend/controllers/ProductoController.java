package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.service.ProductoService;
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

    @PostMapping
    public ResponseEntity<ProductoDTO> crearProducto(@RequestBody ProductoDTO productoDTO) {
        ProductoDTO nuevoProducto = productoService.crearProducto(productoDTO);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizarProducto(@PathVariable Long id, @RequestBody ProductoDTO productoDTO) {
        ProductoDTO productoActualizado = productoService.actualizarProducto(id, productoDTO);
        return ResponseEntity.ok(productoActualizado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtenerPorId(@PathVariable Long id) {
        ProductoDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<ProductoDTO>> obtenerPorEmpresa(@PathVariable Long empresaId) {
        List<ProductoDTO> productos = productoService.obtenerTodosPorEmpresa(empresaId);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/empresa/{empresaId}/activos")
    public ResponseEntity<List<ProductoDTO>> obtenerActivosPorEmpresa(@PathVariable Long empresaId) {
        List<ProductoDTO> productos = productoService.obtenerActivosPorEmpresa(empresaId);
        return ResponseEntity.ok(productos);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}

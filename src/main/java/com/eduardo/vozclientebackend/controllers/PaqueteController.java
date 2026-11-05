package com.eduardo.vozclientebackend.controllers;


import com.eduardo.vozclientebackend.dto.PaqueteDTO;
import com.eduardo.vozclientebackend.service.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paquetes")
@RequiredArgsConstructor
public class PaqueteController {

    private final PaqueteService paqueteService;

    /**
     * Crea un nuevo paquete.
     * Endpoint: POST /api/paquetes
     */
    @PostMapping
    public ResponseEntity<PaqueteDTO> crearPaquete(@RequestBody PaqueteDTO paqueteDTO) {
        PaqueteDTO nuevoPaquete = paqueteService.crearPaquete(paqueteDTO);
        return new ResponseEntity<>(nuevoPaquete, HttpStatus.CREATED);
    }

    /**
     * Actualiza un paquete existente.
     * Endpoint: PUT /api/paquetes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PaqueteDTO> actualizarPaquete(@PathVariable Long id, @RequestBody PaqueteDTO paqueteDTO) {
        PaqueteDTO paqueteActualizado = paqueteService.actualizarPaquete(id, paqueteDTO);
        return ResponseEntity.ok(paqueteActualizado);
    }

    /**
     * Obtiene todos los paquetes (activos e inactivos).
     * Útil para un panel de administración.
     * Endpoint: GET /api/paquetes
     */
    @GetMapping
    public ResponseEntity<List<PaqueteDTO>> obtenerTodos() {
        List<PaqueteDTO> paquetes = paqueteService.obtenerTodos();
        return ResponseEntity.ok(paquetes);
    }

    /**
     * Obtiene solo los paquetes activos.
     * Útil para mostrar en el catálogo a los clientes.
     * Endpoint: GET /api/paquetes/activos
     */
    @GetMapping("/activos")
    public ResponseEntity<List<PaqueteDTO>> obtenerActivos() {
        List<PaqueteDTO> paquetes = paqueteService.obtenerActivos();
        return ResponseEntity.ok(paquetes);
    }

    /**
     * Obtiene un paquete específico por su ID.
     * Endpoint: GET /api/paquetes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaqueteDTO> obtenerPorId(@PathVariable Long id) {
        PaqueteDTO paquete = paqueteService.obtenerPorId(id);
        return ResponseEntity.ok(paquete);
    }

    /**
     * Obtiene un paquete específico por su nombre.
     * Endpoint: GET /api/paquetes/nombre/{nombre}
     */
    @GetMapping("/nombre/{nombre}")
    public ResponseEntity<PaqueteDTO> obtenerPorNombre(@PathVariable String nombre) {
        PaqueteDTO paquete = paqueteService.obtenerPorNombre(nombre);
        return ResponseEntity.ok(paquete);
    }

    /**
     * Desactiva un paquete (Soft Delete).
     * Endpoint: PATCH /api/paquetes/{id}/desactivar
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarPaquete(@PathVariable Long id) {
        paqueteService.desactivarPaquete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina físicamente un paquete.
     * Endpoint: DELETE /api/paquetes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPaquete(@PathVariable Long id) {
        paqueteService.eliminarPaquete(id);
        return ResponseEntity.noContent().build();
    }
}

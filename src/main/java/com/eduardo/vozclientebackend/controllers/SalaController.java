package com.eduardo.vozclientebackend.controllers;


import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.SalaDTO;
import com.eduardo.vozclientebackend.service.SalaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;

    /**
     * POST /api/salas
     * Crea una nueva sala. El token y las fechas se generan automáticamente en el servicio.
     */
    @PreAuthorize("hasAuthority('sala:crear')")
    @PostMapping
    public ResponseEntity<SalaDTO> crearSala(@RequestBody SalaDTO salaDTO) {
        SalaDTO nuevaSala = salaService.crearSala(salaDTO);
        return new ResponseEntity<>(nuevaSala, HttpStatus.CREATED);
    }

    /**
     * GET /api/salas/{token}
     * Obtiene los detalles de una sala mediante su token único.
     */
    @GetMapping("/{token}")
    public ResponseEntity<SalaDTO> obtenerPorToken(@PathVariable String token) {
        SalaDTO sala = salaService.obtenerPorToken(token);
        return ResponseEntity.ok(sala);
    }

    /**
     * GET /api/salas/producto/{productoId}
     * Lista todas las salas asociadas a un producto.
     */
    @PreAuthorize("hasAuthority('sala:leer')")
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<SalaDTO>> obtenerPorProducto(@PathVariable Long productoId) {
        List<SalaDTO> salas = salaService.obtenerPorProducto(productoId);
        return ResponseEntity.ok(salas);
    }

    /**
     * PATCH /api/salas/{token}/responder
     * Actualiza el estado de la sala a RESPONDIDO e ingresa el nombre del consumidor.
     * Usamos PATCH porque es una actualización parcial.
     */
    @PatchMapping("/{token}/responder")
    public ResponseEntity<SalaDTO> responderSala(
        @PathVariable String token,
        @RequestParam String nombreConsumidor) {
        SalaDTO salaActualizada = salaService.responderSala(token, nombreConsumidor);
        return ResponseEntity.ok(salaActualizada);
    }
}

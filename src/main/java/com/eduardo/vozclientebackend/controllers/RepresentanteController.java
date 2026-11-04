package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.CrearRepresentanteDTO;
import com.eduardo.vozclientebackend.dto.CuentaDTO;
import com.eduardo.vozclientebackend.service.RepresentanteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/representantes")
@RequiredArgsConstructor
public class RepresentanteController {

    private final RepresentanteService representanteService;

    /**
     * POST /api/representantes. Registra un representante.
     */
    @PostMapping
    public ResponseEntity<CuentaDTO> crearRepresentante(
        @Valid @RequestBody CrearRepresentanteDTO dto) {

        CuentaDTO representante =
            representanteService.crearRepresentante(dto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(representante);
    }

    /**
     * GET /api/representantes. Lista los representantes de la empresa autenticada.
     */
    @GetMapping
    public ResponseEntity<List<CuentaDTO>> listarRepresentantes() {

        List<CuentaDTO> representantes =
            representanteService.listarRepresentantes();

        return ResponseEntity.ok(representantes);
    }

    /**
     * GET /api/representantes/{id}. Obtiene los datos de un representante.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CuentaDTO> obtenerRepresentante(
        @PathVariable Long id) {

        CuentaDTO representante =
            representanteService.obtenerRepresentante(id);

        return ResponseEntity.ok(representante);
    }

    /**
     * PATCH /api/representantes/{id}/desactivar. Desactiva un representante.
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<CuentaDTO> desactivarRepresentante(
        @PathVariable Long id) {

        CuentaDTO representante =
            representanteService.desactivarRepresentante(id);

        return ResponseEntity.ok(representante);
    }
}

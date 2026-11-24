package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.service.EliminacionService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final EliminacionService eliminacion;

    /**
     * POST /api/representantes. Registra un representante.
     */
    @PreAuthorize("hasAuthority('representante:gestionar')")
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
    @PreAuthorize("hasAuthority('representante:gestionar')")
    @GetMapping
    public ResponseEntity<List<CuentaDTO>> listarRepresentantes() {

        List<CuentaDTO> representantes =
            representanteService.listarRepresentantes();

        return ResponseEntity.ok(representantes);
    }

    /**
     * GET /api/representantes/{id}. Obtiene los datos de un representante.
     */
    @PreAuthorize("hasAuthority('representante:gestionar')")
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
    @PreAuthorize("hasAuthority('representante:gestionar')")
    @PostMapping("/{id}/contrasena-temporal")
    public ResponseEntity<CuentaDTO> reenviarContrasenaTemporal(
        @PathVariable Long id) {
        return ResponseEntity.ok(
            representanteService.reenviarContrasenaTemporal(id));
    }

    @PreAuthorize("hasAuthority('representante:gestionar')")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<CuentaDTO> activarRepresentante(
        @PathVariable Long id) {

        return ResponseEntity.ok(representanteService.activarRepresentante(id));
    }

    @PreAuthorize("hasAuthority('representante:gestionar')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<CuentaDTO> desactivarRepresentante(
        @PathVariable Long id) {

        CuentaDTO representante =
            representanteService.desactivarRepresentante(id);

        return ResponseEntity.ok(representante);
    }

    /** Elimina un representante solo si no tiene datos relacionados; si los tiene responde 409 con el detalle. */
    @PreAuthorize("hasAuthority('datos:eliminar')")
    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRepresentante(@PathVariable Long id) {
        eliminacion.eliminarRepresentante(id);
        return ResponseEntity.noContent().build();
    }
}

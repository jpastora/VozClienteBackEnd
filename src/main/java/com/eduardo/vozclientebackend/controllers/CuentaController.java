package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.CambioRolDTO;
import com.eduardo.vozclientebackend.dto.CambiarRolRequestDTO;
import com.eduardo.vozclientebackend.dto.CuentaDTO;

import com.eduardo.vozclientebackend.service.CuentaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    /**
     * GET /api/cuentas. Lista las cuentas de la empresa.
     */
    @GetMapping
    public ResponseEntity<List<CuentaDTO>> listarCuentas() {

        List<CuentaDTO> cuentas = cuentaService.listarCuentas();

        return ResponseEntity.ok(cuentas);
    }

    /**
     * PATCH /api/cuentas/{id}/rol. Cambia el rol de una cuenta.
     */
    @PatchMapping("/{id}/rol")
    public ResponseEntity<CuentaDTO> cambiarRol(
        @PathVariable Long id,
        @Valid @RequestBody CambiarRolRequestDTO request) {

        CuentaDTO cuentaActualizada =
            cuentaService.cambiarRol(id, request);

        return ResponseEntity.ok(cuentaActualizada);
    }

    /**
     * GET /api/cuentas/{id}/cambios-rol. Consulta el historial de cambios de rol.
     */
    @GetMapping("/{id}/cambios-rol")
    public ResponseEntity<List<CambioRolDTO>> obtenerHistorial(
        @PathVariable Long id) {

        List<CambioRolDTO> historial =
            cuentaService.obtenerHistorial(id);

        return ResponseEntity.ok(historial);
    }
}

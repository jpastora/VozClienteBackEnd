package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.CambiarEstadoRequestDTO;
import com.eduardo.vozclientebackend.dto.CambiarRolRequestDTO;
import com.eduardo.vozclientebackend.dto.CambioRolDTO;
import com.eduardo.vozclientebackend.dto.CrearCuentaDTO;
import com.eduardo.vozclientebackend.dto.CuentaGlobalDTO;
import com.eduardo.vozclientebackend.service.CuentaService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Módulo de usuarios del administrador de la plataforma (RF-02). */
@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    /** POST /api/cuentas. Crea una cuenta con cualquier rol y envía la contraseña temporal. */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @PostMapping
    public ResponseEntity<CuentaGlobalDTO> crearCuenta(@Valid @RequestBody CrearCuentaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.crearCuenta(dto));
    }

    /** GET /api/cuentas?empresaId=. Lista las cuentas de todas las empresas (o de una). */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @GetMapping
    public ResponseEntity<List<CuentaGlobalDTO>> listarCuentas(
        @RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(cuentaService.listarCuentas(empresaId));
    }

    /** PATCH /api/cuentas/{id}/rol. Cambia el rol de una cuenta. */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @PatchMapping("/{id}/rol")
    public ResponseEntity<CuentaGlobalDTO> cambiarRol(
        @PathVariable Long id, @Valid @RequestBody CambiarRolRequestDTO request) {
        return ResponseEntity.ok(cuentaService.cambiarRol(id, request));
    }

    /** PATCH /api/cuentas/{id}/estado. Activa o desactiva una cuenta. */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CuentaGlobalDTO> cambiarEstado(
        @PathVariable Long id, @Valid @RequestBody CambiarEstadoRequestDTO request) {
        return ResponseEntity.ok(cuentaService.cambiarEstado(id, request));
    }

    /** POST /api/cuentas/{id}/contrasena-temporal. Reenvía una contraseña temporal nueva. */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @PostMapping("/{id}/contrasena-temporal")
    public ResponseEntity<CuentaGlobalDTO> reenviarContrasenaTemporal(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.reenviarContrasenaTemporal(id));
    }

    /** GET /api/cuentas/{id}/cambios-rol. Historial de cambios de rol. */
    @PreAuthorize("hasAuthority('cuenta:gestionar')")
    @GetMapping("/{id}/cambios-rol")
    public ResponseEntity<List<CambioRolDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.obtenerHistorial(id));
    }
}

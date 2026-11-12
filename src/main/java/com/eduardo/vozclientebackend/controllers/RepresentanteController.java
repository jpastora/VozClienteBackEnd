package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.ContrasenaDTO;
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
     * PATCH /api/representantes/{id}/contrasena. Asigna o cambia la contraseña de un representante,
     * para que ingrese con correo y contraseña sin cuenta de Google.
     */
    @PreAuthorize("hasAuthority('representante:gestionar')")
    @PatchMapping("/{id}/contrasena")
    public ResponseEntity<CuentaDTO> asignarContrasena(
        @PathVariable Long id,
        @Valid @RequestBody ContrasenaDTO dto) {

        return ResponseEntity.ok(representanteService.asignarContrasena(id, dto.contrasena()));
    }

    /**
     * PATCH /api/representantes/{id}/desactivar. Desactiva un representante.
     */
    @PreAuthorize("hasAuthority('representante:gestionar')")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<CuentaDTO> desactivarRepresentante(
        @PathVariable Long id) {

        CuentaDTO representante =
            representanteService.desactivarRepresentante(id);

        return ResponseEntity.ok(representante);
    }
}

package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.GuiaDTO;
import com.eduardo.vozclientebackend.service.GuiaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaController {

    private final GuiaService guiaService;

    @PreAuthorize("hasAuthority('guia:crear')")
    @PostMapping
    public ResponseEntity<GuiaDTO> crearGuia(@Valid @RequestBody GuiaDTO guiaDTO) {
        GuiaDTO nuevaGuia = guiaService.crearGuia(guiaDTO);
        return new ResponseEntity<>(nuevaGuia, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('guia:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<GuiaDTO> obtenerGuiaPorId(@PathVariable Long id) {
        GuiaDTO guia = guiaService.obtenerGuiaPorId(id);
        return ResponseEntity.ok(guia);
    }

    @PreAuthorize("hasAuthority('guia:leer')")
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<GuiaDTO>> obtenerGuiasPorEmpresa(@PathVariable Long empresaId) {
        List<GuiaDTO> guias = guiaService.obtenerGuiasPorEmpresa(empresaId);
        return ResponseEntity.ok(guias);
    }

    @PreAuthorize("hasAuthority('guia:gestionar')")
    @PutMapping("/{id}")
    public ResponseEntity<GuiaDTO> actualizarGuia(
        @PathVariable Long id,
        @Valid @RequestBody GuiaDTO guiaDTO) {
        GuiaDTO guiaActualizada = guiaService.actualizarGuia(id, guiaDTO);
        return ResponseEntity.ok(guiaActualizada);
    }

    @PreAuthorize("hasAuthority('guia:gestionar')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        guiaService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }
}

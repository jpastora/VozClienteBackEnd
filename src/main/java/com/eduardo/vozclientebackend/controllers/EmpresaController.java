package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.EmpresaDTO;
import com.eduardo.vozclientebackend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    @PreAuthorize("hasAuthority('empresa:gestionar')")
    @PostMapping
    public ResponseEntity<EmpresaDTO> crearEmpresa(@RequestBody EmpresaDTO empresaDTO) {
        EmpresaDTO nuevaEmpresa = empresaService.crearEmpresa(empresaDTO);
        return new ResponseEntity<>(nuevaEmpresa, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('empresa:gestionar')")
    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> obtenerTodasLasEmpresas() {
        List<EmpresaDTO> empresas = empresaService.obtenerTodas();
        return ResponseEntity.ok(empresas);
    }

    @PreAuthorize("hasAuthority('empresa:gestionar')")
    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDTO> obtenerEmpresaPorId(@PathVariable Long id) {
        EmpresaDTO empresa = empresaService.obtenerPorId(id);
        return ResponseEntity.ok(empresa);
    }

    @PreAuthorize("hasAuthority('empresa:gestionar')")
    @PutMapping("/{id}")
    public ResponseEntity<EmpresaDTO> actualizarEmpresa(
        @PathVariable Long id,
        @RequestBody EmpresaDTO empresaDTO) {
        EmpresaDTO empresaActualizada = empresaService.actualizarEmpresa(id, empresaDTO);
        return ResponseEntity.ok(empresaActualizada);
    }

    @PreAuthorize("hasAuthority('empresa:gestionar')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEmpresa(@PathVariable Long id) {
        empresaService.eliminarEmpresa(id);
        return ResponseEntity.noContent().build(); // Devuelve 204 No Content cuando se elimina con éxito
    }
}

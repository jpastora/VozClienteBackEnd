package com.eduardo.vozclientebackend.controllers;

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

    @PostMapping
    public ResponseEntity<EmpresaDTO> crearEmpresa(@RequestBody EmpresaDTO empresaDTO) {
        EmpresaDTO nuevaEmpresa = empresaService.crearEmpresa(empresaDTO);
        return new ResponseEntity<>(nuevaEmpresa, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> obtenerTodasLasEmpresas() {
        List<EmpresaDTO> empresas = empresaService.obtenerTodas();
        return ResponseEntity.ok(empresas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaDTO> obtenerEmpresaPorId(@PathVariable Long id) {
        EmpresaDTO empresa = empresaService.obtenerPorId(id);
        return ResponseEntity.ok(empresa);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaDTO> actualizarEmpresa(
        @PathVariable Long id,
        @RequestBody EmpresaDTO empresaDTO) {
        EmpresaDTO empresaActualizada = empresaService.actualizarEmpresa(id, empresaDTO);
        return ResponseEntity.ok(empresaActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEmpresa(@PathVariable Long id) {
        empresaService.eliminarEmpresa(id);
        return ResponseEntity.noContent().build(); // Devuelve 204 No Content cuando se elimina con éxito
    }
}

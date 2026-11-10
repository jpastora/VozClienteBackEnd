package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.EntrevistaDTO;
import com.eduardo.vozclientebackend.service.EntrevistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entrevistas")
@RequiredArgsConstructor
public class EntrevistaController {

    private final EntrevistaService entrevistaService;

    /**
     * POST /api/entrevistas
     * Inicia una nueva entrevista. Valida que la sala y el representante existan.
     * El estado inicial (EN_CURSO) y la fecha de inicio se asignan automáticamente.
     */
    @PreAuthorize("hasAuthority('entrevista:gestionar')")
    @PostMapping
    public ResponseEntity<EntrevistaDTO> iniciarEntrevista(@RequestBody EntrevistaDTO entrevistaDTO) {
        EntrevistaDTO nuevaEntrevista = entrevistaService.iniciarEntrevista(entrevistaDTO);
        return new ResponseEntity<>(nuevaEntrevista, HttpStatus.CREATED);
    }

    /**
     * GET /api/entrevistas/{id}
     * Obtiene los detalles de una entrevista específica por su ID.
     */
    @PreAuthorize("hasAuthority('entrevista:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<EntrevistaDTO> obtenerPorId(@PathVariable Long id) {
        EntrevistaDTO entrevista = entrevistaService.obtenerPorId(id);
        return ResponseEntity.ok(entrevista);
    }

    /**
     * GET /api/entrevistas/sala/{salaId}
     * Obtiene el historial de todas las entrevistas realizadas en una sala específica.
     */
    @PreAuthorize("hasAuthority('entrevista:leer')")
    @GetMapping("/sala/{salaId}")
    public ResponseEntity<List<EntrevistaDTO>> obtenerPorSala(@PathVariable Long salaId) {
        List<EntrevistaDTO> entrevistas = entrevistaService.obtenerPorSala(salaId);
        return ResponseEntity.ok(entrevistas);
    }

    /**
     * GET /api/entrevistas/representante/{representanteId}
     * Obtiene todas las entrevistas que han sido atendidas por un representante (cuenta) en particular.
     */
    @PreAuthorize("hasAuthority('entrevista:leer')")
    @GetMapping("/representante/{representanteId}")
    public ResponseEntity<List<EntrevistaDTO>> obtenerPorRepresentante(@PathVariable Long representanteId) {
        List<EntrevistaDTO> entrevistas = entrevistaService.obtenerPorRepresentante(representanteId);
        return ResponseEntity.ok(entrevistas);
    }

    /**
     * PATCH /api/entrevistas/{id}/finalizar
     * Marca una entrevista como FINALIZADA y calcula automáticamente su duración.
     * Se usa PATCH porque es una actualización parcial de los datos (estado, finEn, duracion).
     */
    @PreAuthorize("hasAuthority('entrevista:gestionar')")
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<EntrevistaDTO> finalizarEntrevista(@PathVariable Long id) {
        EntrevistaDTO entrevistaFinalizada = entrevistaService.finalizarEntrevista(id);
        return ResponseEntity.ok(entrevistaFinalizada);
    }

    /**
     * PATCH /api/entrevistas/{id}/fallo
     * Marca una entrevista como FALLIDA en caso de desconexión o error, calculando el tiempo que duró.
     */
    @PreAuthorize("hasAuthority('entrevista:gestionar')")
    @PatchMapping("/{id}/fallo")
    public ResponseEntity<EntrevistaDTO> reportarFallo(@PathVariable Long id) {
        EntrevistaDTO entrevistaFallida = entrevistaService.reportarFallo(id);
        return ResponseEntity.ok(entrevistaFallida);
    }
}

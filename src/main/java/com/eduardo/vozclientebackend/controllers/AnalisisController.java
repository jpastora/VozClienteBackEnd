package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.AnalisisDTO;
import com.eduardo.vozclientebackend.security.AccesoEmpresa;
import com.eduardo.vozclientebackend.dto.MencionCompetenciaDTO;
import com.eduardo.vozclientebackend.service.AnalisisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analisis")
public class AnalisisController {

    private final AnalisisService analisisService;
    private final AccesoEmpresa acceso;

    public AnalisisController(AnalisisService analisisService, AccesoEmpresa acceso) {
        this.analisisService = analisisService;
        this.acceso = acceso;
    }

    /**
     * Dispara la IA para analizar la transcripción (Intención, Sentimiento y Competencia).
     * POST /api/v1/analisis/transcripciones/5
     */
    @PreAuthorize("hasAuthority('analisis:procesar')")
    @PostMapping("/transcripciones/{transcripcionId}")
    public ResponseEntity<AnalisisDTO> procesarAnalisis(@PathVariable Long transcripcionId) {
        acceso.exigirTranscripcion(transcripcionId);
        AnalisisDTO resultado = analisisService.procesarAnalisis(transcripcionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    /**
     * Obtiene un análisis previamente guardado sin volver a llamar a la IA.
     * GET /api/v1/analisis/transcripciones/5
     */
    @PreAuthorize("hasAuthority('analisis:leer')")
    @GetMapping("/transcripciones/{transcripcionId}")
    public ResponseEntity<AnalisisDTO> obtenerAnalisisPorTranscripcion(@PathVariable Long transcripcionId) {
        acceso.exigirTranscripcion(transcripcionId);
        AnalisisDTO resultado = analisisService.obtenerAnalisis(transcripcionId);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Obtiene SOLO las menciones de la competencia de un análisis.
     * GET /api/v1/analisis/transcripciones/5/menciones
     */
    @PreAuthorize("hasAuthority('analisis:leer')")
    @GetMapping("/transcripciones/{transcripcionId}/menciones")
    public ResponseEntity<List<MencionCompetenciaDTO>> obtenerMencionesCompetencia(@PathVariable Long transcripcionId) {
        acceso.exigirTranscripcion(transcripcionId);
        List<MencionCompetenciaDTO> menciones = analisisService.obtenerMencionesPorTranscripcion(transcripcionId);

        if (menciones.isEmpty()) {
            return ResponseEntity.noContent().build(); // Devuelve 204 si no hay menciones
        }
        return ResponseEntity.ok(menciones); // Devuelve 200 con la lista JSON
    }
}

package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.TranscripcionDTO;
import com.eduardo.vozclientebackend.service.TranscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transcripciones")
@RequiredArgsConstructor
public class TranscripcionController {

    private final TranscripcionService transcripcionService;

    /** Solicita la transcripción de una grabación. Responde 202 y procesa en segundo plano. */
    @PostMapping("/grabacion/{grabacionId}")
    public ResponseEntity<TranscripcionDTO> solicitar(@PathVariable Long grabacionId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(transcripcionService.solicitarTranscripcion(grabacionId));
    }

    /** Reintenta una transcripción que quedó CON_ERROR (misma lógica que solicitar). */
    @PostMapping("/grabacion/{grabacionId}/reintentar")
    public ResponseEntity<TranscripcionDTO> reintentar(@PathVariable Long grabacionId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(transcripcionService.solicitarTranscripcion(grabacionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscripcionDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(transcripcionService.obtenerPorId(id));
    }

    /** Útil para que el frontend haga polling del estado. */
    @GetMapping("/grabacion/{grabacionId}")
    public ResponseEntity<TranscripcionDTO> obtenerPorGrabacion(@PathVariable Long grabacionId) {
        return ResponseEntity.ok(transcripcionService.obtenerPorGrabacionId(grabacionId));
    }
}

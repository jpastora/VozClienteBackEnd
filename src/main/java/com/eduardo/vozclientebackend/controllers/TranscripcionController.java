package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.security.AccesoEmpresa;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final AccesoEmpresa acceso;

    private final TranscripcionService transcripcionService;

    /** Solicita la transcripción de una grabación. Responde 202 y procesa en segundo plano. */
    @PreAuthorize("hasAuthority('transcripcion:gestionar')")
    @PostMapping("/grabacion/{grabacionId}")
    public ResponseEntity<TranscripcionDTO> solicitar(@PathVariable Long grabacionId) {
        acceso.exigirGrabacion(grabacionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(transcripcionService.solicitarTranscripcion(grabacionId));
    }

    /** Reintenta una transcripción que quedó CON_ERROR (misma lógica que solicitar). */
    @PreAuthorize("hasAuthority('transcripcion:gestionar')")
    @PostMapping("/grabacion/{grabacionId}/reintentar")
    public ResponseEntity<TranscripcionDTO> reintentar(@PathVariable Long grabacionId) {
        acceso.exigirGrabacion(grabacionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(transcripcionService.solicitarTranscripcion(grabacionId));
    }

    @PreAuthorize("hasAuthority('transcripcion:leer')")
    @GetMapping("/{id}")
    public ResponseEntity<TranscripcionDTO> obtenerPorId(@PathVariable Long id) {
        acceso.exigirTranscripcion(id);
        return ResponseEntity.ok(transcripcionService.obtenerPorId(id));
    }

    /** Útil para que el frontend haga polling del estado. */
    @PreAuthorize("hasAuthority('transcripcion:leer')")
    @GetMapping("/grabacion/{grabacionId}")
    public ResponseEntity<TranscripcionDTO> obtenerPorGrabacion(@PathVariable Long grabacionId) {
        acceso.exigirGrabacion(grabacionId);
        return ResponseEntity.ok(transcripcionService.obtenerPorGrabacionId(grabacionId));
    }
}

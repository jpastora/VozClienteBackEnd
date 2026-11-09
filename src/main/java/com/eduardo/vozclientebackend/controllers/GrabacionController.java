package com.eduardo.vozclientebackend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.GrabacionDTO;
import com.eduardo.vozclientebackend.service.GrabacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/grabaciones")
@RequiredArgsConstructor
public class GrabacionController {

    private final GrabacionService grabacionService;

    /**
     * Endpoint para subir el archivo .webm generado por WebRTC.
     * Al recibir un archivo físico, el Content-Type debe ser 'multipart/form-data'.
     */
    @PreAuthorize("hasAuthority('grabacion:subir')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GrabacionDTO> subirGrabacion(
        @RequestParam("entrevistaId") Long entrevistaId,
        @RequestParam("archivo") MultipartFile archivo,
        @RequestParam(value = "duracionSegundos", defaultValue = "0") Integer duracionSegundos,
        @RequestParam(value = "tieneAudioCliente", defaultValue = "true") Boolean tieneAudioCliente,
        @RequestParam(value = "esParcial", defaultValue = "false") Boolean esParcial
    ) {
        GrabacionDTO nuevaGrabacion = grabacionService.crearYGuardarGrabacionWebm(
            entrevistaId,
            archivo,
            duracionSegundos,
            tieneAudioCliente,
            esParcial
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaGrabacion);
    }

    /**
     * Obtiene los metadatos de la grabación asociada a una entrevista.
     */
    @PreAuthorize("hasAuthority('grabacion:leer')")
    @GetMapping("/entrevista/{entrevistaId}")
    public ResponseEntity<GrabacionDTO> obtenerPorEntrevistaId(@PathVariable Long entrevistaId) {
        GrabacionDTO grabacion = grabacionService.obtenerPorEntrevistaId(entrevistaId);
        return ResponseEntity.ok(grabacion);
    }
}

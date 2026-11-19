package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.security.AccesoEmpresa;

import org.springframework.security.access.prepost.PreAuthorize;
import com.eduardo.vozclientebackend.dto.FinalizarGrabacionDTO;
import com.eduardo.vozclientebackend.dto.FragmentoAckDTO;
import com.eduardo.vozclientebackend.dto.GrabacionDTO;
import com.eduardo.vozclientebackend.enums.CanalGrabacion;
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

    private final AccesoEmpresa acceso;

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
        acceso.exigirEntrevista(entrevistaId);
        GrabacionDTO nuevaGrabacion = grabacionService.crearYGuardarGrabacionWebm(
            entrevistaId,
            archivo,
            duracionSegundos,
            tieneAudioCliente,
            esParcial
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaGrabacion);
    }

    /** POST /api/v1/grabaciones/{entrevistaId}/iniciar: crea la grabación en estado GRABANDO (idempotente). */
    @PreAuthorize("hasAuthority('grabacion:subir')")
    @PostMapping("/{entrevistaId}/iniciar")
    public ResponseEntity<GrabacionDTO> iniciar(@PathVariable Long entrevistaId) {
        acceso.exigirEntrevista(entrevistaId);
        return ResponseEntity.ok(grabacionService.iniciarGrabacion(entrevistaId));
    }

    /**
     * POST /api/v1/grabaciones/{entrevistaId}/fragmentos: recibe un fragmento de un canal.
     * 409 con secuenciaEsperada si falta alguno; 409 sin ella si la grabación ya se cerró.
     */
    @PreAuthorize("hasAuthority('grabacion:subir')")
    @PostMapping(value = "/{entrevistaId}/fragmentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FragmentoAckDTO> recibirFragmento(
        @PathVariable Long entrevistaId,
        @RequestParam("canal") CanalGrabacion canal,
        @RequestParam("secuencia") Integer secuencia,
        @RequestParam("fragmento") MultipartFile fragmento,
        @RequestParam(value = "hashSha256", required = false) String hashSha256
    ) {
        acceso.exigirEntrevista(entrevistaId);
        return ResponseEntity.ok(
            grabacionService.recibirFragmento(entrevistaId, canal, secuencia, fragmento, hashSha256));
    }

    /** POST /api/v1/grabaciones/{entrevistaId}/finalizar: cierra la grabación (idempotente). */
    @PreAuthorize("hasAuthority('grabacion:subir')")
    @PostMapping("/{entrevistaId}/finalizar")
    public ResponseEntity<GrabacionDTO> finalizar(
        @PathVariable Long entrevistaId,
        @RequestBody FinalizarGrabacionDTO cuerpo
    ) {
        acceso.exigirEntrevista(entrevistaId);
        return ResponseEntity.ok(grabacionService.finalizarGrabacion(
            entrevistaId, cuerpo.duracionSegundos(), cuerpo.tieneAudioCliente(), cuerpo.esParcial()));
    }

    /**
     * Obtiene los metadatos de la grabación asociada a una entrevista.
     */
    @PreAuthorize("hasAuthority('grabacion:leer')")
    @GetMapping("/entrevista/{entrevistaId}")
    public ResponseEntity<GrabacionDTO> obtenerPorEntrevistaId(@PathVariable Long entrevistaId) {
        acceso.exigirEntrevista(entrevistaId);
        GrabacionDTO grabacion = grabacionService.obtenerPorEntrevistaId(entrevistaId);
        return ResponseEntity.ok(grabacion);
    }
}

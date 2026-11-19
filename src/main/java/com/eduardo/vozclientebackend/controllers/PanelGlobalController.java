package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.PanelGlobalDTO;
import com.eduardo.vozclientebackend.service.PanelGlobalService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Panel global de ingresos y estadísticas de la plataforma (RF-19). */
@RestController
@RequestMapping("/api/panel-global")
@RequiredArgsConstructor
public class PanelGlobalController {

    private final PanelGlobalService panelGlobalService;

    /** GET /api/panel-global. Solo el administrador de la plataforma. */
    @PreAuthorize("hasAuthority('panel:global')")
    @GetMapping
    public ResponseEntity<PanelGlobalDTO> obtener() {
        return ResponseEntity.ok(panelGlobalService.obtener());
    }
}

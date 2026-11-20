package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.ConsumoSalaDTO;
import com.eduardo.vozclientebackend.dto.SaldoSalasDTO;
import com.eduardo.vozclientebackend.service.SaldoSalasService;
import com.eduardo.vozclientebackend.service.UsuarioActualService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Saldo de salas e historial de consumo de la empresa con sesión (RF-17, RF-18).
 *
 * <p>La empresa siempre es la de la sesión; el administrador de la plataforma la elige con la
 * cabecera {@code X-Empresa-Id}. No se recibe ningún id de empresa en la URL.</p>
 */
@RestController
@RequestMapping("/api/saldo")
@RequiredArgsConstructor
public class SaldoController {

    private final UsuarioActualService usuarioActualService;
    private final SaldoSalasService saldoSalasService;

    /**
     * GET /api/saldo: salas acreditadas, consumidas, reservadas, saldo y disponibles.
     *
     * @return saldo de la empresa
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping
    public ResponseEntity<SaldoSalasDTO> obtener() {
        return ResponseEntity.ok(saldoSalasService.obtener(usuarioActualService.empresaEfectiva()));
    }

    /**
     * GET /api/saldo/consumos: salas descontadas, la más reciente primero.
     *
     * @return historial de consumo
     */
    @PreAuthorize("hasAuthority('compra:leer')")
    @GetMapping("/consumos")
    public ResponseEntity<List<ConsumoSalaDTO>> consumos() {
        return ResponseEntity.ok(saldoSalasService.consumos(usuarioActualService.empresaEfectiva()));
    }
}

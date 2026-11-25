package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.EstadisticasDTO;
import com.eduardo.vozclientebackend.service.EstadisticasService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @PreAuthorize("hasAuthority('analisis:leer')")
    @GetMapping
    public ResponseEntity<EstadisticasDTO> obtenerEstadisticas(

        @RequestParam(required = false)
        Long productoId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        )
        LocalDate desde,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        )
        LocalDate hasta

    ) {

        EstadisticasDTO estadisticas =
            estadisticasService.obtenerEstadisticas(

                productoId,

                desde,

                hasta

            );

        return ResponseEntity.ok(estadisticas);
    }
}

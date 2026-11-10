package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.SesionDTO;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.service.UsuarioActualService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión del usuario (HU-01).
 *
 * <ul>
 *   <li>Ingreso: el front-end navega a {@code /oauth2/authorization/google}.</li>
 *   <li>{@code GET /api/auth/yo}: datos de la persona con sesión; 401 si no hay sesión.</li>
 *   <li>{@code POST /api/auth/salir}: cierra la sesión (lo atiende Spring Security, ver
 *       {@code SecurityConfig}); responde 204.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioActualService usuarioActualService;

    /**
     * Devuelve la persona con sesión iniciada.
     *
     * @return los datos de la sesión con el rol vigente
     */
    @GetMapping("/yo")
    public ResponseEntity<SesionDTO> obtenerSesion() {
        Cuenta cuenta = usuarioActualService.obtenerCuentaActual();
        return ResponseEntity.ok(new SesionDTO(
            cuenta.getId(),
            cuenta.getNombreCompleto(),
            cuenta.getCorreo(),
            cuenta.getRol(),
            cuenta.getEmpresaId()));
    }
}

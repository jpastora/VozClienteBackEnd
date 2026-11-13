package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.dto.IngresoDTO;
import com.eduardo.vozclientebackend.dto.RegistroDTO;
import com.eduardo.vozclientebackend.dto.SesionDTO;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.security.CookieSesionService;
import com.eduardo.vozclientebackend.security.JwtService;
import com.eduardo.vozclientebackend.service.CredencialesService;
import com.eduardo.vozclientebackend.service.UsuarioActualService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión del usuario (HU-01).
 *
 * <ul>
 *   <li>Ingreso con Google: el front-end navega a {@code /oauth2/authorization/google}.</li>
 *   <li>{@code POST /api/auth/ingresar}: ingreso con correo y contraseña.</li>
 *   <li>{@code POST /api/auth/registro}: crea la empresa y la cuenta de cliente-administrador.</li>
 *   <li>{@code GET /api/auth/yo}: datos de la persona con sesión; 401 si no hay sesión.</li>
 *   <li>{@code POST /api/auth/salir}: cierra la sesión (lo atiende Spring Security, ver
 *       {@code SecurityConfig}); responde 204.</li>
 * </ul>
 *
 * <p>Las tres formas de ingreso dejan la misma sesión: un JWT propio en la cookie HttpOnly
 * {@code VC_SESION}.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioActualService usuarioActualService;
    private final CredencialesService credencialesService;
    private final JwtService jwtService;
    private final CookieSesionService cookieSesionService;

    /**
     * Devuelve la persona con sesión iniciada.
     *
     * @return los datos de la sesión con el rol vigente
     */
    @GetMapping("/yo")
    public ResponseEntity<SesionDTO> obtenerSesion() {
        return ResponseEntity.ok(aSesion(usuarioActualService.obtenerCuentaActual()));
    }

    /**
     * Ingresa con correo y contraseña y abre la sesión.
     *
     * @param datos    correo y contraseña
     * @param response respuesta, donde se escribe la cookie de sesión
     * @return los datos de la sesión; 401 si las credenciales no son válidas, 403 si la cuenta está inactiva
     */
    @PostMapping("/ingresar")
    public ResponseEntity<SesionDTO> ingresar(@Valid @RequestBody IngresoDTO datos, HttpServletResponse response) {
        Cuenta cuenta = credencialesService.ingresar(datos);
        abrirSesion(cuenta, response);
        return ResponseEntity.ok(aSesion(cuenta));
    }

    /**
     * Crea la empresa y la cuenta de cliente-administrador con correo y contraseña, y abre la sesión.
     *
     * @param datos    nombre, correo y contraseña
     * @param response respuesta, donde se escribe la cookie de sesión
     * @return 201 con los datos de la sesión; 409 si el correo ya existe, 400 si la contraseña no cumple
     */
    @PostMapping("/registro")
    public ResponseEntity<SesionDTO> registrar(@Valid @RequestBody RegistroDTO datos, HttpServletResponse response) {
        Cuenta cuenta = credencialesService.registrar(datos);
        abrirSesion(cuenta, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(aSesion(cuenta));
    }

    private void abrirSesion(Cuenta cuenta, HttpServletResponse response) {
        cookieSesionService.escribir(response, jwtService.emitir(cuenta.getId()), jwtService.getDuracionInactividad());
    }

    private static SesionDTO aSesion(Cuenta cuenta) {
        return new SesionDTO(
            cuenta.getId(),
            cuenta.getNombreCompleto(),
            cuenta.getCorreo(),
            cuenta.getRol(),
            cuenta.getEmpresaId());
    }
}

package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioActualService {

    /** Atributo del principal con el id de la cuenta; lo pone el filtro de la cookie de sesión. */
    public static final String ATRIBUTO_CUENTA_ID = "cuentaId";

    /** Cabecera con la empresa que el administrador de la plataforma atiende (modo soporte). */
    public static final String CABECERA_EMPRESA = "X-Empresa-Id";

    private final CuentaRepository cuentaRepository;
    private final EmpresaRepository empresaRepository;

    /**
     * Empresa sobre la que se opera en esta solicitud.
     *
     * <ul>
     *   <li>Cliente-administrador y representante: la de su cuenta.</li>
     *   <li>Administrador de la plataforma (sin empresa): la indicada en la cabecera
     *       {@code X-Empresa-Id}, que debe existir.</li>
     * </ul>
     */
    public Long empresaEfectiva() {
        Cuenta cuenta = obtenerCuentaActual();

        if (cuenta.getRol() != RolCuenta.ADMINISTRADOR) {
            if (cuenta.getEmpresaId() == null) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El usuario no tiene empresa asignada");
            }
            return cuenta.getEmpresaId();
        }

        String valor = leerCabeceraEmpresa();
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Selecciona la empresa con la que vas a trabajar");
        }

        long id;
        try {
            id = Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "La empresa seleccionada no es válida");
        }

        if (!empresaRepository.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "La empresa seleccionada no existe");
        }
        return id;
    }

    /**
     * Indica si la cuenta actual puede operar sobre la empresa dada: el administrador de la
     * plataforma sobre cualquiera; el resto, solo sobre la suya.
     */
    public boolean puedeAccederAEmpresa(Long empresaId) {
        Cuenta cuenta = obtenerCuentaActual();
        if (cuenta.getRol() == RolCuenta.ADMINISTRADOR) {
            return true;
        }
        return empresaId != null && empresaId.equals(cuenta.getEmpresaId());
    }

    private static String leerCabeceraEmpresa() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(CABECERA_EMPRESA);
        }
        return null;
    }

    /**
     * Obtiene la cuenta asociada al usuario autenticado.
     */
    public Cuenta obtenerCuentaActual() {

        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "El usuario debe iniciar sesión"
            );
        }

        String googleSub = null;

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {

            // Sesión de la cookie (Google o correo y contraseña): se busca por el id de la cuenta.
            Object cuentaId = oauth2User.getAttribute(ATRIBUTO_CUENTA_ID);

            if (cuentaId instanceof Long id) {
                return cuentaRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "La cuenta autenticada no está registrada"
                    ));
            }

            // Perfil dev-test: identidad fija por el sub de Google.
            Object sub = oauth2User.getAttribute("sub");

            if (sub != null) {
                googleSub = sub.toString();
            }
        }

        if (googleSub == null || googleSub.isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar la cuenta de Google"
            );
        }

        Cuenta cuenta = cuentaRepository.findByGoogleSub(googleSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "La cuenta autenticada no está registrada"
            ));

        return cuenta;
    }
}

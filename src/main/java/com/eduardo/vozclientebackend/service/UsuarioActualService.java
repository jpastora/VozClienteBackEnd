package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioActualService {

    /** Atributo del principal con el id de la cuenta; lo pone el filtro de la cookie de sesión. */
    public static final String ATRIBUTO_CUENTA_ID = "cuentaId";

    private final CuentaRepository cuentaRepository;

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

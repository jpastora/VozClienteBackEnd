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

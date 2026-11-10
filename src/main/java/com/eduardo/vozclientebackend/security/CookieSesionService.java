package com.eduardo.vozclientebackend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Lee, escribe y borra la cookie que transporta el JWT de la sesión (HU-01).
 *
 * <p>La cookie es HttpOnly (JavaScript no la puede leer), SameSite=Lax (no viaja en
 * solicitudes POST de otros sitios) y Secure cuando la aplicación se sirve por HTTPS.</p>
 */
@Service
public class CookieSesionService {

    /** Nombre de la cookie de sesión. */
    public static final String NOMBRE_COOKIE = "VC_SESION";

    private final boolean cookieSegura;

    /**
     * Crea el servicio.
     *
     * @param cookieSegura {@code true} para marcar la cookie como Secure (HTTPS, por ejemplo con ngrok)
     */
    public CookieSesionService(@Value("${app.seguridad.cookie-segura:false}") boolean cookieSegura) {
        this.cookieSegura = cookieSegura;
    }

    /**
     * Agrega a la respuesta la cookie con el token de sesión.
     *
     * @param response respuesta HTTP
     * @param token    JWT firmado
     * @param duracion tiempo de vida de la cookie
     */
    public void escribir(HttpServletResponse response, String token, Duration duracion) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir(token, duracion).toString());
    }

    /**
     * Agrega a la respuesta una cookie vencida que borra la sesión del navegador.
     *
     * @param response respuesta HTTP
     */
    public void borrar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, construir("", Duration.ZERO).toString());
    }

    /**
     * Obtiene el token de la cookie de sesión, si viene en la solicitud.
     *
     * @param request solicitud HTTP
     * @return el token, o vacío si no hay cookie
     */
    public Optional<String> leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (NOMBRE_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie construir(String valor, Duration duracion) {
        return ResponseCookie.from(NOMBRE_COOKIE, valor)
            .httpOnly(true)
            .secure(cookieSegura)
            .sameSite("Lax")
            .path("/")
            .maxAge(duracion)
            .build();
    }
}

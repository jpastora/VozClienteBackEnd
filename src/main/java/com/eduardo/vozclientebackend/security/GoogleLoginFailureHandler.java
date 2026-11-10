package com.eduardo.vozclientebackend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Se ejecuta cuando el ingreso con Google no se completa (HU-01): el usuario canceló el
 * consentimiento o Google no respondió. Redirige a la pantalla de ingreso con un código
 * para que el front-end muestre un mensaje claro y permita reintentar, sin datos técnicos.
 */
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleLoginFailureHandler.class);

    private final String urlFrontend;

    /**
     * Crea el manejador.
     *
     * @param urlFrontend URL base del front-end, sin barra final
     */
    public GoogleLoginFailureHandler(String urlFrontend) {
        this.urlFrontend = urlFrontend;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String codigo = "google_no_disponible";
        if (exception instanceof OAuth2AuthenticationException oauth2
            && "access_denied".equals(oauth2.getError().getErrorCode())) {
            codigo = "consentimiento_cancelado";
        }
        LOG.info("Ingreso con Google fallido: {}", exception.getMessage());
        response.sendRedirect(urlFrontend + "/auth?error=" + codigo);
    }
}

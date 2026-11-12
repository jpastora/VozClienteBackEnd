package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.service.RegistroGoogleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Se ejecuta cuando Google confirma la identidad del usuario (HU-01).
 *
 * <p>Registra el ingreso (crea la empresa y la cuenta en el primer ingreso), emite el JWT
 * propio en una cookie HttpOnly y redirige al front-end. Si el ingreso se rechaza, redirige
 * a la pantalla de ingreso con el código del motivo.</p>
 */
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleLoginSuccessHandler.class);

    private final RegistroGoogleService registroGoogleService;
    private final JwtService jwtService;
    private final CookieSesionService cookieSesionService;
    private final String urlFrontend;

    /**
     * Crea el manejador.
     *
     * @param registroGoogleService servicio que crea o vincula la cuenta
     * @param jwtService            emisor del JWT de la sesión
     * @param cookieSesionService   escritura de la cookie de sesión
     * @param urlFrontend           URL base del front-end, sin barra final
     */
    public GoogleLoginSuccessHandler(RegistroGoogleService registroGoogleService, JwtService jwtService,
                                     CookieSesionService cookieSesionService, String urlFrontend) {
        this.registroGoogleService = registroGoogleService;
        this.jwtService = jwtService;
        this.cookieSesionService = cookieSesionService;
        this.urlFrontend = urlFrontend;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // La sesión HTTP solo se necesitó durante el intercambio con Google.
        HttpSession sesionHttp = request.getSession(false);
        if (sesionHttp != null) {
            sesionHttp.invalidate();
        }

        if (!(authentication.getPrincipal() instanceof OAuth2User usuarioGoogle)) {
            response.sendRedirect(urlFrontend + "/auth?error=datos_incompletos");
            return;
        }

        try {
            Cuenta cuenta = registroGoogleService.registrarIngreso(
                texto(usuarioGoogle.getAttribute("sub")),
                texto(usuarioGoogle.getAttribute("email")),
                texto(usuarioGoogle.getAttribute("name")),
                esVerdadero(usuarioGoogle.getAttribute("email_verified")));

            String token = jwtService.emitir(cuenta.getId());
            cookieSesionService.escribir(response, token, jwtService.getDuracionInactividad());
            // Se entra a /app: el front lleva a cada perfil a su pantalla de inicio.
            response.sendRedirect(urlFrontend + "/app");

        } catch (IngresoRechazadoException e) {
            LOG.info("Ingreso con Google rechazado: {}", e.getMotivo());
            cookieSesionService.borrar(response);
            response.sendRedirect(urlFrontend + "/auth?error=" + e.getMotivo().getCodigo());
        }
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    /** Google envía {@code email_verified} como booleano; algunos proveedores lo envían como texto. */
    private static boolean esVerdadero(Object valor) {
        return Boolean.TRUE.equals(valor) || "true".equalsIgnoreCase(String.valueOf(valor));
    }
}

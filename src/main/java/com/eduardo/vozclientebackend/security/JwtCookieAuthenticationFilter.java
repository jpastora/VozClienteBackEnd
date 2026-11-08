package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Autentica cada solicitud a partir de la cookie de sesión (HU-01).
 *
 * <p>Valida el JWT, busca la cuenta por su identificador de Google y, si está activa,
 * la deja en el contexto de seguridad con el rol vigente en la base de datos. El
 * principal es un {@link DefaultOAuth2User} con el atributo {@code sub}, que es lo que
 * espera {@code UsuarioActualService}. Cuando el token pasó la mitad de su vida, se emite
 * uno nuevo para que la sesión solo expire por inactividad.</p>
 *
 * <p>No se declara como {@code @Component} para que Spring Boot no lo registre dos veces;
 * lo agrega {@link SecurityConfig} a la cadena de seguridad.</p>
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CookieSesionService cookieSesionService;
    private final CuentaRepository cuentaRepository;

    /**
     * Crea el filtro.
     *
     * @param jwtService          validación y emisión del JWT
     * @param cookieSesionService lectura y escritura de la cookie
     * @param cuentaRepository    acceso a las cuentas
     */
    public JwtCookieAuthenticationFilter(JwtService jwtService, CookieSesionService cookieSesionService,
                                         CuentaRepository cuentaRepository) {
        this.jwtService = jwtService;
        this.cookieSesionService = cookieSesionService;
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<String> token = cookieSesionService.leer(request);
        if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token.get(), response);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletResponse response) {
        Optional<JwtService.SesionToken> sesion = jwtService.validar(token);
        if (sesion.isEmpty()) {
            // Token vencido, alterado o firmado con otro secreto: se borra la cookie.
            cookieSesionService.borrar(response);
            return;
        }

        Optional<Cuenta> cuenta = cuentaRepository.findByGoogleSub(sesion.get().googleSub());
        if (cuenta.isEmpty() || cuenta.get().getEstado() != EstadoCuenta.ACTIVA) {
            // La cuenta se desactivó o se eliminó después de iniciar sesión.
            cookieSesionService.borrar(response);
            return;
        }

        Cuenta activa = cuenta.get();
        List<GrantedAuthority> permisos = List.of(new SimpleGrantedAuthority("ROLE_" + activa.getRol().name()));
        DefaultOAuth2User principal = new DefaultOAuth2User(
            permisos,
            Map.of(
                "sub", activa.getGoogleSub(),
                "email", activa.getCorreo(),
                "name", activa.getNombreCompleto()),
            "sub");

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, permisos));
        SecurityContextHolder.setContext(contexto);

        if (jwtService.debeRenovarse(sesion.get())) {
            cookieSesionService.escribir(response, jwtService.emitir(activa.getGoogleSub()),
                jwtService.getDuracionInactividad());
        }
    }
}

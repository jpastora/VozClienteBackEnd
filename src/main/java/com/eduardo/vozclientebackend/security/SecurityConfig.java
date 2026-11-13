package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.service.RegistroGoogleService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Configuración de seguridad (HU-01, RNF-01).
 *
 * <ul>
 *   <li>Ingreso con Google OAuth2 ({@code /oauth2/authorization/google}) o con correo y contraseña
 *       ({@code POST /api/auth/ingresar}; registro en {@code POST /api/auth/registro}).</li>
 *   <li>En ambos casos se emite un JWT propio en la cookie HttpOnly {@code VC_SESION}.</li>
 *   <li>Sin sesión HTTP en el servidor: cada solicitud se autentica con la cookie.</li>
 *   <li>Toda ruta {@code /api/**} exige sesión, salvo las del consumidor, que entra sin cuenta (RF-08).</li>
 *   <li>Sin sesión, la API responde 401 (no redirige a Google); el front-end muestra el ingreso.</li>
 *   <li>Cada endpoint exige su permiso con {@code @PreAuthorize}; sin permiso responde 403
 *       (matriz en {@link PermisosPorRol}, RF-02).</li>
 * </ul>
 *
 * <p>CSRF queda deshabilitado porque la cookie es SameSite=Lax: el navegador no la envía en
 * solicitudes POST, PUT, PATCH ni DELETE que se originen en otros sitios.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Ruta para cerrar la sesión (RF-01: la sesión puede cerrarse manualmente). */
    public static final String RUTA_SALIR = "/api/auth/salir";

    /**
     * Cadena de seguridad principal.
     *
     * @param http                  configurador de Spring Security
     * @param registroGoogleService crea o vincula la cuenta al volver de Google
     * @param jwtService            emisión y validación del JWT
     * @param cookieSesionService   manejo de la cookie de sesión
     * @param cuentaRepository      acceso a las cuentas
     * @param urlFrontend           URL base del front-end
     * @return la cadena de filtros configurada
     * @throws Exception si la configuración no es válida
     */
    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        RegistroGoogleService registroGoogleService,
        JwtService jwtService,
        CookieSesionService cookieSesionService,
        CuentaRepository cuentaRepository,
        @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend) throws Exception {

        String frontend = urlFrontend.replaceAll("/+$", "");

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Flujo de Google y páginas de error
                .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Ingreso y registro con correo y contraseña
                .requestMatchers(HttpMethod.POST, "/api/auth/ingresar", "/api/auth/registro").permitAll()
                // Consumidor sin cuenta: consultar el enlace, aceptar la grabación y la señalización (RF-08, RF-09)
                .requestMatchers(HttpMethod.GET, "/api/salas/*").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/salas/*/responder").permitAll()
                .requestMatchers("/signaling", "/signaling/**").permitAll()
                // Todo lo demás de la API exige sesión
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .successHandler(new GoogleLoginSuccessHandler(
                    registroGoogleService, jwtService, cookieSesionService, frontend))
                .failureHandler(new GoogleLoginFailureHandler(frontend)))
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                // Sin permiso: 403 sin cuerpo, para no revelar datos del recurso (RNF de seguridad).
                .accessDeniedHandler((request, response, denegado) ->
                    response.setStatus(HttpStatus.FORBIDDEN.value())))
            .logout(salir -> salir
                .logoutUrl(RUTA_SALIR)
                .addLogoutHandler((request, response, authentication) -> cookieSesionService.borrar(response))
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .addFilterBefore(
                new JwtCookieAuthenticationFilter(jwtService, cookieSesionService, cuentaRepository),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cifrado de las contraseñas propias de la aplicación con BCrypt.
     *
     * @return el codificador de contraseñas
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

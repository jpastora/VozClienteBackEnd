package com.eduardo.vozclientebackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication
    .UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DevTestAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

        // Identidad fija para pruebas locales.
        // Debe coincidir con google_sub en la base de datos.
        String googleSub = "dev-test-admin";

        // Solo para pruebas locales: el usuario de pruebas recibe todos los permisos
        // de PermisosPorRol para poder llamar cualquier endpoint protegido con @PreAuthorize.
        List<SimpleGrantedAuthority> autoridades = new java.util.ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        PermisosPorRol.todos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));

        OAuth2User usuario = new DefaultOAuth2User(
            autoridades,
            Map.of(
                "sub", googleSub,
                "name", "Administrador de pruebas"
            ),
            "sub"
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                usuario.getAuthorities()
            );

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

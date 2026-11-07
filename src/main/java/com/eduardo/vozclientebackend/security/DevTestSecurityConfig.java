package com.eduardo.vozclientebackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev-test")
public class DevTestSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain devTestSecurityFilterChain(HttpSecurity http)
        throws Exception {

        http
            .securityMatcher("/api/cuentas/**", "/api/representantes/**", "/api/productos/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new DevTestAuthenticationFilter(),
                org.springframework.security.web.authentication
                    .UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}

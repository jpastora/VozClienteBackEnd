package com.eduardo.vozclientebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj compartido de la aplicación. Inyectarlo en lugar de llamar a {@code now()}
 * permite fijar la hora en las pruebas unitarias.
 */
@Configuration
public class RelojConfig {

    /**
     * Reloj del sistema en la zona horaria del servidor.
     *
     * @return el reloj del sistema
     */
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}

package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.security.PoliticaContrasena;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la contraseña temporal de las cuentas creadas por un administrador. */
class ContrasenaTemporalServiceTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T15:00:00Z");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final ContrasenaTemporalService servicio =
        new ContrasenaTemporalService(encoder, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void generar_cumpleLaPoliticaYTieneLargoFijo() {
        for (int i = 0; i < 500; i++) {
            String contrasena = servicio.generar();
            assertThat(contrasena).hasSize(ContrasenaTemporalService.LARGO);
            assertThat(PoliticaContrasena.cumple(contrasena)).as(contrasena).isTrue();
        }
    }

    @Test
    void generar_noRepiteContrasenas() {
        assertThat(servicio.generar()).isNotEqualTo(servicio.generar());
    }

    @Test
    void asignar_guardaSoloElHashMarcaElCambioObligatorioYVenceEnSieteDias() {
        Cuenta cuenta = new Cuenta();

        String contrasena = servicio.asignar(cuenta);

        assertThat(cuenta.getContrasenaHash()).isNotEqualTo(contrasena);
        assertThat(encoder.matches(contrasena, cuenta.getContrasenaHash())).isTrue();
        assertThat(cuenta.isDebeCambiarContrasena()).isTrue();
        assertThat(cuenta.getContrasenaTemporalVenceEn())
            .isEqualTo(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusDays(7));
    }
}

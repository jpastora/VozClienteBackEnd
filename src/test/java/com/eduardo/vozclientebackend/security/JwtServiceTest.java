package com.eduardo.vozclientebackend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del JWT de la sesión (HU-01).
 */
class JwtServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    private static final Instant INICIO = Instant.parse("2026-11-08T12:00:00Z");
    private static final Duration TREINTA_MINUTOS = Duration.ofMinutes(30);

    private static JwtService servicioEn(Instant instante) {
        return new JwtService(SECRETO, TREINTA_MINUTOS, Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void validar_tokenRecienEmitido_devuelveElSub() {
        JwtService servicio = servicioEn(INICIO);

        Optional<JwtService.SesionToken> sesion = servicio.validar(servicio.emitir("google-123"));

        assertThat(sesion).isPresent();
        assertThat(sesion.get().sujeto()).isEqualTo("google-123");
        assertThat(sesion.get().expiraEn()).isEqualTo(INICIO.plus(TREINTA_MINUTOS));
    }

    @Test
    void validar_tokenVencidoPorInactividad_devuelveVacio() {
        String token = servicioEn(INICIO).emitir("google-123");

        Optional<JwtService.SesionToken> sesion = servicioEn(INICIO.plus(Duration.ofMinutes(31))).validar(token);

        assertThat(sesion).isEmpty();
    }

    @Test
    void validar_tokenAlterado_devuelveVacio() {
        JwtService servicio = servicioEn(INICIO);
        String token = servicio.emitir("google-123");
        // Cambia un carácter dentro de los datos (payload): la firma deja de coincidir.
        int posicion = token.indexOf('.') + 5;
        char reemplazo = token.charAt(posicion) == 'A' ? 'B' : 'A';
        String alterado = token.substring(0, posicion) + reemplazo + token.substring(posicion + 1);

        assertThat(servicio.validar(alterado)).isEmpty();
    }

    @Test
    void validar_tokenFirmadoConOtroSecreto_devuelveVacio() {
        JwtService otro = new JwtService("otro-secreto-distinto-con-mas-de-32-caracteres", TREINTA_MINUTOS,
            Clock.fixed(INICIO, ZoneOffset.UTC));

        assertThat(servicioEn(INICIO).validar(otro.emitir("google-123"))).isEmpty();
    }

    @Test
    void validar_textoQueNoEsToken_devuelveVacio() {
        assertThat(servicioEn(INICIO).validar("no-es-un-jwt")).isEmpty();
        assertThat(servicioEn(INICIO).validar("")).isEmpty();
        assertThat(servicioEn(INICIO).validar(null)).isEmpty();
    }

    @Test
    void debeRenovarse_antesDeLaMitadDeLaVida_devuelveFalso() {
        JwtService.SesionToken sesion = servicioEn(INICIO).validar(servicioEn(INICIO).emitir("g")).orElseThrow();

        assertThat(servicioEn(INICIO.plus(Duration.ofMinutes(10))).debeRenovarse(sesion)).isFalse();
    }

    @Test
    void debeRenovarse_despuesDeLaMitadDeLaVida_devuelveVerdadero() {
        JwtService.SesionToken sesion = servicioEn(INICIO).validar(servicioEn(INICIO).emitir("g")).orElseThrow();

        assertThat(servicioEn(INICIO.plus(Duration.ofMinutes(16))).debeRenovarse(sesion)).isTrue();
    }

    @Test
    void cuentaId_tokenEmitidoConIdDeCuenta_devuelveElId() {
        JwtService servicio = servicioEn(INICIO);

        JwtService.SesionToken sesion = servicio.validar(servicio.emitir(42L)).orElseThrow();

        assertThat(sesion.cuentaId()).contains(42L);
    }

    @Test
    void cuentaId_tokenDeVersionAnteriorConSubDeGoogle_devuelveVacio() {
        JwtService servicio = servicioEn(INICIO);

        JwtService.SesionToken sesion = servicio.validar(servicio.emitir("google-123")).orElseThrow();

        assertThat(sesion.cuentaId()).isEmpty();
    }

    @Test
    void constructor_secretoCorto_lanzaExcepcion() {
        assertThatThrownBy(() -> new JwtService("corto", TREINTA_MINUTOS, Clock.systemUTC()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_secretoVacio_generaUnoAleatorioYFunciona() {
        JwtService servicio = new JwtService("", TREINTA_MINUTOS, Clock.fixed(INICIO, ZoneOffset.UTC));

        assertThat(servicio.validar(servicio.emitir("google-123"))).isPresent();
    }
}

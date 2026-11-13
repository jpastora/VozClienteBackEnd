package com.eduardo.vozclientebackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del límite de intentos fallidos de ingreso con contraseña (HU-01).
 */
class LimiteIntentosIngresoTest {

    private static final String CORREO = "ana@empresa.com";

    /** Reloj que la prueba puede adelantar. */
    private static final class RelojAjustable extends Clock {
        private Instant ahora = Instant.parse("2026-11-12T23:00:00Z");

        void adelantar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private RelojAjustable reloj;
    private LimiteIntentosIngreso limite;

    @BeforeEach
    void preparar() {
        reloj = new RelojAjustable();
        limite = new LimiteIntentosIngreso(reloj);
    }

    private void fallar(int veces) {
        for (int i = 0; i < veces; i++) {
            limite.registrarFallo(CORREO);
        }
    }

    @Test
    void verificar_menosFallosQueElMaximo_permiteIntentar() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS - 1);

        assertThatCode(() -> limite.verificar(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void verificar_alLlegarAlMaximo_bloqueaCon429() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS);

        assertThatThrownBy(() -> limite.verificar(CORREO))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(
                ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    void verificar_elBloqueoSoloAfectaAEseCorreo() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS);

        assertThatCode(() -> limite.verificar("otro@empresa.com")).doesNotThrowAnyException();
    }

    @Test
    void verificar_pasadoElBloqueo_vuelveAPermitir() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS);

        reloj.adelantar(LimiteIntentosIngreso.BLOQUEO);

        assertThatCode(() -> limite.verificar(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void registrarFallo_fallosFueraDeLaVentana_noSeAcumulan() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS - 1);
        reloj.adelantar(LimiteIntentosIngreso.VENTANA);

        fallar(1);

        assertThatCode(() -> limite.verificar(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void registrarExito_borraElConteo() {
        fallar(LimiteIntentosIngreso.MAXIMO_FALLOS - 1);

        limite.registrarExito(CORREO);
        fallar(1);

        assertThatCode(() -> limite.verificar(CORREO)).doesNotThrowAnyException();
    }
}

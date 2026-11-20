package com.eduardo.vozclientebackend.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la pasarela de pago simulada (RF-18).
 */
class PagoSimuladoServiceTest {

    private static final UUID CLAVE = UUID.fromString("7a1b2c3d-0000-4000-8000-000000000000");

    private final PagoSimuladoService pago =
        new PagoSimuladoService(Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC));

    private static void esperar400(Runnable accion, String mensaje) {
        assertThatThrownBy(accion::run)
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.BAD_REQUEST.value()))
            .hasMessageContaining(mensaje);
    }

    @Test
    void procesar_tarjetaDePruebaValida_seAprueba() {
        PagoSimuladoService.Resultado r = pago.procesar("Ana Vargas", "4242 4242 4242 4242", "12/28", "123", CLAVE);

        assertThat(r.aprobado()).isTrue();
        assertThat(r.motivo()).isNull();
        assertThat(r.referencia()).isEqualTo("SIM-7A1B2C3D · ****4242");
    }

    @Test
    void procesar_tarjetaDeRechazo_seRechazaConMotivo() {
        PagoSimuladoService.Resultado r = pago.procesar("Ana Vargas", "4000-0000-0000-0002", "12/28", "123", CLAVE);

        assertThat(r.aprobado()).isFalse();
        assertThat(r.motivo()).isEqualTo("Tarjeta rechazada por el banco emisor");
        assertThat(r.referencia()).contains("****0002").contains("rechazada");
    }

    @Test
    void procesar_laReferenciaNoGuardaElNumeroCompletoNiElCvv() {
        PagoSimuladoService.Resultado r = pago.procesar("Ana Vargas", "4242424242424242", "12/28", "987", CLAVE);

        assertThat(r.referencia()).doesNotContain("424242424242").doesNotContain("987");
    }

    @Test
    void procesar_numeroQueNoCumpleLuhn_responde400() {
        esperar400(() -> pago.procesar("Ana Vargas", "4242424242424241", "12/28", "123", CLAVE),
            "número de tarjeta");
    }

    @Test
    void procesar_numeroCorto_responde400() {
        esperar400(() -> pago.procesar("Ana Vargas", "4242", "12/28", "123", CLAVE), "número de tarjeta");
    }

    @Test
    void procesar_tarjetaVencida_responde400() {
        esperar400(() -> pago.procesar("Ana Vargas", "4242424242424242", "10/26", "123", CLAVE), "vencida");
    }

    @Test
    void procesar_vencimientoDelMesActual_esValido() {
        assertThat(pago.procesar("Ana Vargas", "4242424242424242", "11/26", "123", CLAVE).aprobado()).isTrue();
    }

    @Test
    void procesar_vencimientoConFormatoIncorrecto_responde400() {
        esperar400(() -> pago.procesar("Ana Vargas", "4242424242424242", "13/28", "123", CLAVE), "MM/AA");
    }

    @Test
    void procesar_cvvInvalido_responde400() {
        esperar400(() -> pago.procesar("Ana Vargas", "4242424242424242", "12/28", "12a", CLAVE), "CVV");
    }

    @Test
    void procesar_titularConNumeros_responde400() {
        esperar400(() -> pago.procesar("Ana 123", "4242424242424242", "12/28", "123", CLAVE), "titular");
    }

    @Test
    void cumpleLuhn_reconoceLasTarjetasDePrueba() {
        assertThat(PagoSimuladoService.cumpleLuhn("4242424242424242")).isTrue();
        assertThat(PagoSimuladoService.cumpleLuhn("4000000000000002")).isTrue();
        assertThat(PagoSimuladoService.cumpleLuhn("4000000000009995")).isTrue();
        assertThat(PagoSimuladoService.cumpleLuhn("1234567812345678")).isFalse();
    }
}

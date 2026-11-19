package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CambiarContrasenaDTO;
import com.eduardo.vozclientebackend.dto.IngresoDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.security.LimiteIntentosIngreso;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Pruebas de la contraseña temporal: vencimiento en el ingreso y cambio obligatorio. */
@ExtendWith(MockitoExtension.class)
class CredencialesContrasenaTemporalTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T15:00:00Z");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private CredencialesService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        servicio = new CredencialesService(cuentaRepository, empresaRepository, encoder,
            new LimiteIntentosIngreso(reloj), reloj);
        lenient().when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Cuenta cuentaTemporal(OffsetDateTime vence) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(8L);
        cuenta.setCorreo("nueva@empresa.com");
        cuenta.setNombreCompleto("Nueva Persona");
        cuenta.setRol(RolCuenta.REPRESENTANTE);
        cuenta.setEstado(EstadoCuenta.PENDIENTE);
        cuenta.setContrasenaHash(encoder.encode("Temporal123"));
        cuenta.setDebeCambiarContrasena(true);
        cuenta.setContrasenaTemporalVenceEn(vence);
        return cuenta;
    }

    @Test
    void ingresar_conTemporalVigente_activaLaCuentaYMantieneElCambioObligatorio() {
        Cuenta cuenta = cuentaTemporal(OffsetDateTime.parse("2026-12-05T00:00:00Z"));
        when(cuentaRepository.buscarPorCorreo("nueva@empresa.com")).thenReturn(Optional.of(cuenta));

        Cuenta resultado = servicio.ingresar(new IngresoDTO("nueva@empresa.com", "Temporal123"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(resultado.isDebeCambiarContrasena()).isTrue();
    }

    @Test
    void ingresar_conTemporalVencida_responde401() {
        Cuenta cuenta = cuentaTemporal(OffsetDateTime.parse("2026-11-30T00:00:00Z"));
        when(cuentaRepository.buscarPorCorreo("nueva@empresa.com")).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> servicio.ingresar(new IngresoDTO("nueva@empresa.com", "Temporal123")))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void cambiarContrasena_quitaLaMarcaYElVencimiento() {
        Cuenta cuenta = cuentaTemporal(OffsetDateTime.parse("2026-12-05T00:00:00Z"));

        Cuenta resultado = servicio.cambiarContrasena(cuenta, new CambiarContrasenaDTO("Temporal123", "Definitiva456"));

        assertThat(resultado.isDebeCambiarContrasena()).isFalse();
        assertThat(resultado.getContrasenaTemporalVenceEn()).isNull();
        assertThat(encoder.matches("Definitiva456", resultado.getContrasenaHash())).isTrue();
    }

    @Test
    void cambiarContrasena_conActualIncorrecta_responde401() {
        Cuenta cuenta = cuentaTemporal(null);

        assertThatThrownBy(() -> servicio.cambiarContrasena(cuenta, new CambiarContrasenaDTO("Otra123456", "Definitiva456")))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void cambiarContrasena_nuevaQueNoCumpleLaPolitica_responde400() {
        Cuenta cuenta = cuentaTemporal(null);

        assertThatThrownBy(() -> servicio.cambiarContrasena(cuenta, new CambiarContrasenaDTO("Temporal123", "corta")))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void cambiarContrasena_nuevaIgualALaActual_responde400() {
        Cuenta cuenta = cuentaTemporal(null);

        assertThatThrownBy(() -> servicio.cambiarContrasena(cuenta, new CambiarContrasenaDTO("Temporal123", "Temporal123")))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}

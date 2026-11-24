package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la recuperación de contraseña ("¿Olvidó su contraseña?").
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionContrasenaServiceTest {

    private static final String CORREO = "ana@empresa.com";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private CorreoService correoService;

    private RecuperacionContrasenaService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-24T02:00:00Z"), ZoneOffset.UTC);
        servicio = new RecuperacionContrasenaService(cuentaRepository,
            new ContrasenaTemporalService(encoder, reloj), correoService, reloj, Runnable::run);
    }

    private Cuenta cuenta(EstadoCuenta estado, String googleSub) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(5L);
        cuenta.setCorreo(CORREO);
        cuenta.setNombreCompleto("Ana Vargas");
        cuenta.setRol(RolCuenta.CLIENTE_ADMINISTRADOR);
        cuenta.setEstado(estado);
        cuenta.setGoogleSub(googleSub);
        cuenta.setContrasenaHash(encoder.encode("ClaveVieja1"));
        return cuenta;
    }

    @Test
    void solicitar_cuentaConContrasena_asignaTemporalYEnviaElCorreo() {
        Cuenta cuenta = cuenta(EstadoCuenta.ACTIVA, null);
        when(cuentaRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(cuenta));
        when(correoService.enviarRecuperacionContrasena(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);

        servicio.solicitar("  Ana@Empresa.com ");

        ArgumentCaptor<String> temporal = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarRecuperacionContrasena(eq(CORREO), eq("Ana Vargas"), temporal.capture(),
            any(OffsetDateTime.class));
        assertThat(encoder.matches(temporal.getValue(), cuenta.getContrasenaHash())).isTrue();
        assertThat(encoder.matches("ClaveVieja1", cuenta.getContrasenaHash())).isFalse();
        assertThat(cuenta.isDebeCambiarContrasena()).isTrue();
        assertThat(cuenta.getContrasenaTemporalVenceEn()).isNotNull();
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    void solicitar_correoInexistente_noHaceNadaNiLanzaError() {
        when(cuentaRepository.buscarPorCorreo("nadie@empresa.com")).thenReturn(Optional.empty());

        servicio.solicitar("nadie@empresa.com");

        verify(cuentaRepository, never()).save(any());
        verify(correoService, never()).enviarRecuperacionContrasena(any(), any(), any(), any());
    }

    @Test
    void solicitar_cuentaDeGoogle_noRecibeContrasena() {
        when(cuentaRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(cuenta(EstadoCuenta.ACTIVA, "g-1")));

        servicio.solicitar(CORREO);

        verify(cuentaRepository, never()).save(any());
        verify(correoService, never()).enviarRecuperacionContrasena(any(), any(), any(), any());
    }

    @Test
    void solicitar_cuentaInactiva_noRecibeContrasena() {
        when(cuentaRepository.buscarPorCorreo(CORREO)).thenReturn(Optional.of(cuenta(EstadoCuenta.INACTIVA, null)));

        servicio.solicitar(CORREO);

        verify(cuentaRepository, never()).save(any());
        verify(correoService, never()).enviarRecuperacionContrasena(any(), any(), any(), any());
    }

    @Test
    void solicitar_masDeTresVecesSeguidas_lasDemasSeIgnoran() {
        when(cuentaRepository.buscarPorCorreo(CORREO)).thenAnswer(i -> Optional.of(cuenta(EstadoCuenta.ACTIVA, null)));
        when(correoService.enviarRecuperacionContrasena(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);

        for (int i = 0; i < RecuperacionContrasenaService.MAXIMO_SOLICITUDES + 2; i++) {
            servicio.solicitar(CORREO);
        }

        verify(correoService, times(RecuperacionContrasenaService.MAXIMO_SOLICITUDES))
            .enviarRecuperacionContrasena(anyString(), anyString(), anyString(), any());
    }

    @Test
    void solicitar_correoVacio_noHaceNada() {
        servicio.solicitar("   ");

        verify(cuentaRepository, never()).buscarPorCorreo(any());
    }
}

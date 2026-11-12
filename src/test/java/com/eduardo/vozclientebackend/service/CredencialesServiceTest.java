package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.IngresoDTO;
import com.eduardo.vozclientebackend.dto.RegistroDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;

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
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del ingreso y registro con correo y contraseña (HU-01).
 */
@ExtendWith(MockitoExtension.class)
class CredencialesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-12T03:00:00Z");

    /** Costo bajo solo para que las pruebas sean rápidas; la aplicación usa el costo por defecto. */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private CredencialesService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CredencialesService(cuentaRepository, empresaRepository, encoder,
            Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private Cuenta cuentaConContrasena(String contrasena, EstadoCuenta estado) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(5L);
        cuenta.setCorreo("ana@empresa.com");
        cuenta.setNombreCompleto("Ana Vargas");
        cuenta.setRol(RolCuenta.REPRESENTANTE);
        cuenta.setEstado(estado);
        cuenta.setContrasenaHash(contrasena == null ? null : encoder.encode(contrasena));
        return cuenta;
    }

    private static HttpStatus estadoDe(Throwable error) {
        return HttpStatus.valueOf(((ResponseStatusException) error).getStatusCode().value());
    }

    @Test
    void ingresar_contrasenaCorrecta_devuelveLaCuentaYRegistraElIngreso() {
        Cuenta cuenta = cuentaConContrasena("Clave2026", EstadoCuenta.ACTIVA);
        when(cuentaRepository.buscarPorCorreo("ana@empresa.com")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        Cuenta resultado = servicio.ingresar(new IngresoDTO(" Ana@Empresa.com ", "Clave2026"));

        assertThat(resultado.getId()).isEqualTo(5L);
        assertThat(resultado.getUltimoIngresoEn()).isNotNull();
    }

    @Test
    void ingresar_cuentaPendiente_laActivaEnSuPrimerIngreso() {
        Cuenta cuenta = cuentaConContrasena("Clave2026", EstadoCuenta.PENDIENTE);
        when(cuentaRepository.buscarPorCorreo("ana@empresa.com")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        Cuenta resultado = servicio.ingresar(new IngresoDTO("ana@empresa.com", "Clave2026"));

        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test
    void ingresar_contrasenaIncorrecta_responde401ConMensajeGenerico() {
        when(cuentaRepository.buscarPorCorreo("ana@empresa.com"))
            .thenReturn(Optional.of(cuentaConContrasena("Clave2026", EstadoCuenta.ACTIVA)));

        assertThatThrownBy(() -> servicio.ingresar(new IngresoDTO("ana@empresa.com", "OtraClave1")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.UNAUTHORIZED))
            .hasMessageContaining(CredencialesService.CREDENCIALES_INVALIDAS);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void ingresar_correoInexistente_respondeElMismoMensajeQueContrasenaIncorrecta() {
        when(cuentaRepository.buscarPorCorreo("nadie@empresa.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.ingresar(new IngresoDTO("nadie@empresa.com", "Clave2026")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.UNAUTHORIZED))
            .hasMessageContaining(CredencialesService.CREDENCIALES_INVALIDAS);
    }

    @Test
    void ingresar_cuentaSoloDeGoogle_responde401() {
        when(cuentaRepository.buscarPorCorreo("ana@empresa.com"))
            .thenReturn(Optional.of(cuentaConContrasena(null, EstadoCuenta.ACTIVA)));

        assertThatThrownBy(() -> servicio.ingresar(new IngresoDTO("ana@empresa.com", "Clave2026")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void ingresar_cuentaInactiva_responde403() {
        when(cuentaRepository.buscarPorCorreo("ana@empresa.com"))
            .thenReturn(Optional.of(cuentaConContrasena("Clave2026", EstadoCuenta.INACTIVA)));

        assertThatThrownBy(() -> servicio.ingresar(new IngresoDTO("ana@empresa.com", "Clave2026")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrar_correoNuevo_creaEmpresaYClienteAdministradorConContrasenaCifrada() {
        when(cuentaRepository.existsByCorreoIgnoreCase("luis@tienda.com")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> {
            Empresa empresa = i.getArgument(0);
            empresa.setId(20L);
            return empresa;
        });
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));

        Cuenta cuenta = servicio.registrar(new RegistroDTO("Luis Mora", "Luis@Tienda.com", "Clave2026"));

        assertThat(cuenta.getEmpresaId()).isEqualTo(20L);
        assertThat(cuenta.getCorreo()).isEqualTo("luis@tienda.com");
        assertThat(cuenta.getRol()).isEqualTo(RolCuenta.CLIENTE_ADMINISTRADOR);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getContrasenaHash()).isNotEqualTo("Clave2026");
        assertThat(encoder.matches("Clave2026", cuenta.getContrasenaHash())).isTrue();
    }

    @Test
    void registrar_correoYaRegistrado_responde409SinCrearNada() {
        when(cuentaRepository.existsByCorreoIgnoreCase("ana@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new RegistroDTO("Ana", "ana@empresa.com", "Clave2026")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(empresaRepository, never()).save(any());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrar_contrasenaSinNumeros_responde400() {
        assertThatThrownBy(() -> servicio.registrar(new RegistroDTO("Luis", "luis@tienda.com", "SoloLetras")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrar_contrasenaSinMayuscula_responde400() {
        assertThatThrownBy(() -> servicio.registrar(new RegistroDTO("Luis", "luis@tienda.com", "clave2026")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrar_contrasenaDeMasDe20Caracteres_responde400() {
        assertThatThrownBy(() -> servicio.registrar(
                new RegistroDTO("Luis", "luis@tienda.com", "Clave2026Clave2026Clave")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}

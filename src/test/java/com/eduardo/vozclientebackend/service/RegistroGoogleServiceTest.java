package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.security.IngresoRechazadoException;
import com.eduardo.vozclientebackend.security.IngresoRechazadoException.Motivo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del registro de ingreso con Google (HU-01).
 */
@ExtendWith(MockitoExtension.class)
class RegistroGoogleServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-08T18:00:00Z");

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private RegistroGoogleService servicio;

    @BeforeEach
    void preparar() {
        servicio = new RegistroGoogleService(cuentaRepository, empresaRepository,
            Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private void guardarDevuelveLoMismo() {
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Cuenta cuenta(String googleSub, RolCuenta rol, EstadoCuenta estado) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(7L);
        cuenta.setEmpresaId(3L);
        cuenta.setNombreCompleto("Laura Rojas");
        cuenta.setCorreo("lrojas@atlas.com");
        cuenta.setGoogleSub(googleSub);
        cuenta.setRol(rol);
        cuenta.setEstado(estado);
        return cuenta;
    }

    @Test
    void registrarIngreso_primerIngreso_creaEmpresaYClienteAdministrador() {
        when(cuentaRepository.findByGoogleSub("g-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarPorCorreo("ana@gmail.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacion -> {
            Empresa empresa = invocacion.getArgument(0);
            empresa.setId(10L);
            return empresa;
        });
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-1", "Ana@Gmail.com", "Ana Vargas", true);

        assertThat(resultado.getEmpresaId()).isEqualTo(10L);
        assertThat(resultado.getCorreo()).isEqualTo("ana@gmail.com");
        assertThat(resultado.getNombreCompleto()).isEqualTo("Ana Vargas");
        assertThat(resultado.getGoogleSub()).isEqualTo("g-1");
        assertThat(resultado.getRol()).isEqualTo(RolCuenta.CLIENTE_ADMINISTRADOR);
        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(resultado.getUltimoIngresoEn()).isEqualTo(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC));
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    void registrarIngreso_cuentaYaVinculada_actualizaUltimoIngresoSinCrearEmpresa() {
        Cuenta existente = cuenta("g-1", RolCuenta.CLIENTE_ADMINISTRADOR, EstadoCuenta.ACTIVA);
        when(cuentaRepository.findByGoogleSub("g-1")).thenReturn(Optional.of(existente));
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-1", "lrojas@atlas.com", "Laura", true);

        assertThat(resultado).isSameAs(existente);
        assertThat(resultado.getUltimoIngresoEn()).isNotNull();
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void registrarIngreso_representantePendiente_vinculaYActivaConservandoElRol() {
        Cuenta representante = cuenta(null, RolCuenta.REPRESENTANTE, EstadoCuenta.PENDIENTE);
        when(cuentaRepository.findByGoogleSub("g-2")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarPorCorreo("lrojas@atlas.com")).thenReturn(Optional.of(representante));
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-2", "lrojas@atlas.com", "Laura Rojas", true);

        assertThat(resultado.getGoogleSub()).isEqualTo("g-2");
        assertThat(resultado.getRol()).isEqualTo(RolCuenta.REPRESENTANTE);
        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void registrarIngreso_cuentaConContrasenaSinVincular_vinculaYBorraLaContrasena() {
        Cuenta conContrasena = cuenta(null, RolCuenta.CLIENTE_ADMINISTRADOR, EstadoCuenta.ACTIVA);
        conContrasena.setContrasenaHash("$2a$10$hashDeQuienRegistroElCorreo");
        when(cuentaRepository.findByGoogleSub("g-3")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarPorCorreo("lrojas@atlas.com")).thenReturn(Optional.of(conContrasena));
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-3", "lrojas@atlas.com", "Laura Rojas", true);

        assertThat(resultado.getGoogleSub()).isEqualTo("g-3");
        assertThat(resultado.getContrasenaHash()).isNull();
    }

    @Test
    void registrarIngreso_cuentaYaVinculadaConContrasena_conservaLaContrasena() {
        Cuenta vinculada = cuenta("g-1", RolCuenta.CLIENTE_ADMINISTRADOR, EstadoCuenta.ACTIVA);
        vinculada.setContrasenaHash("$2a$10$hashPuestoDespuesDeVincular");
        when(cuentaRepository.findByGoogleSub("g-1")).thenReturn(Optional.of(vinculada));
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-1", "lrojas@atlas.com", "Laura", true);

        assertThat(resultado.getContrasenaHash()).isEqualTo("$2a$10$hashPuestoDespuesDeVincular");
    }

    @Test
    void registrarIngreso_correoNoVerificado_rechazaElIngreso() {
        assertThatThrownBy(() -> servicio.registrarIngreso("g-1", "ana@gmail.com", "Ana", false))
            .isInstanceOf(IngresoRechazadoException.class)
            .extracting(e -> ((IngresoRechazadoException) e).getMotivo())
            .isEqualTo(Motivo.CORREO_NO_VERIFICADO);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrarIngreso_cuentaInactiva_rechazaElIngreso() {
        when(cuentaRepository.findByGoogleSub("g-1"))
            .thenReturn(Optional.of(cuenta("g-1", RolCuenta.REPRESENTANTE, EstadoCuenta.INACTIVA)));

        assertThatThrownBy(() -> servicio.registrarIngreso("g-1", "lrojas@atlas.com", "Laura", true))
            .isInstanceOf(IngresoRechazadoException.class)
            .extracting(e -> ((IngresoRechazadoException) e).getMotivo())
            .isEqualTo(Motivo.CUENTA_INACTIVA);
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void registrarIngreso_correoVinculadoAOtraCuentaDeGoogle_rechazaElIngreso() {
        when(cuentaRepository.findByGoogleSub("g-nuevo")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarPorCorreo("lrojas@atlas.com"))
            .thenReturn(Optional.of(cuenta("g-original", RolCuenta.REPRESENTANTE, EstadoCuenta.ACTIVA)));

        assertThatThrownBy(() -> servicio.registrarIngreso("g-nuevo", "lrojas@atlas.com", "Laura", true))
            .isInstanceOf(IngresoRechazadoException.class)
            .extracting(e -> ((IngresoRechazadoException) e).getMotivo())
            .isEqualTo(Motivo.CUENTA_VINCULADA_A_OTRO);
    }

    @Test
    void registrarIngreso_sinCorreo_rechazaPorDatosIncompletos() {
        assertThatThrownBy(() -> servicio.registrarIngreso("g-1", null, "Ana", true))
            .isInstanceOf(IngresoRechazadoException.class)
            .extracting(e -> ((IngresoRechazadoException) e).getMotivo())
            .isEqualTo(Motivo.DATOS_INCOMPLETOS);
    }

    @Test
    void registrarIngreso_sinNombreDeGoogle_usaLaParteLocalDelCorreo() {
        when(cuentaRepository.findByGoogleSub("g-1")).thenReturn(Optional.empty());
        when(cuentaRepository.buscarPorCorreo("ana.vargas@gmail.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        guardarDevuelveLoMismo();

        Cuenta resultado = servicio.registrarIngreso("g-1", "ana.vargas@gmail.com", " ", true);

        assertThat(resultado.getNombreCompleto()).isEqualTo("ana.vargas");
    }
}

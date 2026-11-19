package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CambiarEstadoRequestDTO;
import com.eduardo.vozclientebackend.dto.CambiarRolRequestDTO;
import com.eduardo.vozclientebackend.dto.CrearCuentaDTO;
import com.eduardo.vozclientebackend.dto.CuentaGlobalDTO;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.CambioRol;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CambioRolRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del módulo de usuarios del administrador de la plataforma (RF-02). */
@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T15:00:00Z");

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private CambioRolRepository cambioRolRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @Mock
    private CorreoService correoService;

    private CuentaService servicio;
    private Cuenta admin;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        ContrasenaTemporalService temporal = new ContrasenaTemporalService(new BCryptPasswordEncoder(4), reloj);
        servicio = new CuentaService(cuentaRepository, cambioRolRepository, empresaRepository,
            usuarioActualService, temporal, correoService, reloj);

        admin = cuenta(1L, RolCuenta.ADMINISTRADOR, null, EstadoCuenta.ACTIVA, 3);
        lenient().when(usuarioActualService.obtenerCuentaActual()).thenReturn(admin);
        lenient().when(cuentaRepository.saveAndFlush(any(Cuenta.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(correoService.enviarContrasenaTemporal(anyString(), anyString(), anyString(), any()))
            .thenReturn(true);
    }

    private static Cuenta cuenta(Long id, RolCuenta rol, Long empresaId, EstadoCuenta estado, Integer version) {
        Cuenta c = new Cuenta();
        c.setId(id);
        c.setRol(rol);
        c.setEmpresaId(empresaId);
        c.setEstado(estado);
        c.setVersion(version);
        c.setNombreCompleto("Persona " + id);
        c.setCorreo("p" + id + "@x.com");
        c.setUltimoIngresoEn(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
        return c;
    }

    private static void assertEstado(Throwable e, HttpStatus esperado) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class,
            r -> assertThat(r.getStatusCode()).isEqualTo(esperado));
    }

    // ---- crear ----

    @Test
    void crearCuenta_administradorIgnoraLaEmpresaYNaceSinEllaPendienteConContrasenaTemporal() {
        when(cuentaRepository.existsByCorreoIgnoreCase("nuevo@x.com")).thenReturn(false);

        CuentaGlobalDTO dto = servicio.crearCuenta(
            new CrearCuentaDTO("Nuevo Admin", " Nuevo@X.com ", RolCuenta.ADMINISTRADOR, 9L));

        assertThat(dto.empresaId()).isNull();
        assertThat(dto.rol()).isEqualTo(RolCuenta.ADMINISTRADOR);
        assertThat(dto.estado()).isEqualTo(EstadoCuenta.PENDIENTE);
        assertThat(dto.correo()).isEqualTo("nuevo@x.com");
        assertThat(dto.contrasenaTemporalPendiente()).isTrue();
        assertThat(dto.correoEnviado()).isTrue();
    }

    @Test
    void crearCuenta_rolConEmpresa_exigeQueLaEmpresaExista() {
        when(cuentaRepository.existsByCorreoIgnoreCase("a@x.com")).thenReturn(false);
        when(empresaRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crearCuenta(
            new CrearCuentaDTO("Ana Vargas", "a@x.com", RolCuenta.REPRESENTANTE, 9L)))
            .satisfies(e -> assertEstado(e, HttpStatus.NOT_FOUND));
        verify(cuentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearCuenta_rolConEmpresaSinEmpresa_responde400() {
        when(cuentaRepository.existsByCorreoIgnoreCase("a@x.com")).thenReturn(false);

        assertThatThrownBy(() -> servicio.crearCuenta(
            new CrearCuentaDTO("Ana Vargas", "a@x.com", RolCuenta.CLIENTE_ADMINISTRADOR, null)))
            .satisfies(e -> assertEstado(e, HttpStatus.BAD_REQUEST));
    }

    @Test
    void crearCuenta_correoRepetido_responde409() {
        when(cuentaRepository.existsByCorreoIgnoreCase("a@x.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearCuenta(
            new CrearCuentaDTO("Ana Vargas", "a@x.com", RolCuenta.REPRESENTANTE, 1L)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    @Test
    void crearCuenta_siElCorreoFallaLaCuentaSeCreaIgualYSeInforma() {
        when(cuentaRepository.existsByCorreoIgnoreCase("a@x.com")).thenReturn(false);
        when(empresaRepository.existsById(1L)).thenReturn(true);
        when(correoService.enviarContrasenaTemporal(anyString(), anyString(), anyString(), any())).thenReturn(false);

        CuentaGlobalDTO dto = servicio.crearCuenta(
            new CrearCuentaDTO("Ana Vargas", "a@x.com", RolCuenta.REPRESENTANTE, 1L));

        assertThat(dto.correoEnviado()).isFalse();
        verify(cuentaRepository).saveAndFlush(any(Cuenta.class));
    }

    @Test
    void cualquierOperacion_sinSerAdministrador_responde403() {
        when(usuarioActualService.obtenerCuentaActual())
            .thenReturn(cuenta(2L, RolCuenta.CLIENTE_ADMINISTRADOR, 1L, EstadoCuenta.ACTIVA, 0));

        assertThatThrownBy(() -> servicio.listarCuentas(null)).satisfies(e -> assertEstado(e, HttpStatus.FORBIDDEN));
    }

    // ---- cambiar rol ----

    @Test
    void cambiarRol_promoverAAdministrador_quitaLaEmpresaYRegistraLaBitacora() {
        Cuenta objetivo = cuenta(5L, RolCuenta.CLIENTE_ADMINISTRADOR, 2L, EstadoCuenta.ACTIVA, 4);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        CuentaGlobalDTO dto = servicio.cambiarRol(5L, new CambiarRolRequestDTO(RolCuenta.ADMINISTRADOR, 4, 2L));

        assertThat(dto.rol()).isEqualTo(RolCuenta.ADMINISTRADOR);
        assertThat(dto.empresaId()).isNull();
        ArgumentCaptor<CambioRol> cambio = ArgumentCaptor.forClass(CambioRol.class);
        verify(cambioRolRepository).save(cambio.capture());
        assertThat(cambio.getValue().getRolAnterior()).isEqualTo("CLIENTE_ADMINISTRADOR");
        assertThat(cambio.getValue().getRolNuevo()).isEqualTo("ADMINISTRADOR");
        assertThat(cambio.getValue().getAutorId()).isEqualTo(1L);
    }

    @Test
    void cambiarRol_degradarAdministrador_exigeElegirEmpresa() {
        Cuenta otroAdmin = cuenta(6L, RolCuenta.ADMINISTRADOR, null, EstadoCuenta.ACTIVA, 0);
        when(cuentaRepository.findById(6L)).thenReturn(Optional.of(otroAdmin));
        when(cuentaRepository.countByRolAndEstadoAndEmpresaIdIsNull(RolCuenta.ADMINISTRADOR, EstadoCuenta.ACTIVA))
            .thenReturn(2L);

        assertThatThrownBy(() -> servicio.cambiarRol(6L,
            new CambiarRolRequestDTO(RolCuenta.CLIENTE_ADMINISTRADOR, 0, null)))
            .satisfies(e -> assertEstado(e, HttpStatus.BAD_REQUEST));
        verify(cambioRolRepository, never()).save(any());
    }

    @Test
    void cambiarRol_degradarAdministradorConEmpresa_asignaLaEmpresa() {
        Cuenta otroAdmin = cuenta(6L, RolCuenta.ADMINISTRADOR, null, EstadoCuenta.ACTIVA, 0);
        when(cuentaRepository.findById(6L)).thenReturn(Optional.of(otroAdmin));
        when(cuentaRepository.countByRolAndEstadoAndEmpresaIdIsNull(RolCuenta.ADMINISTRADOR, EstadoCuenta.ACTIVA))
            .thenReturn(2L);
        when(empresaRepository.existsById(3L)).thenReturn(true);

        CuentaGlobalDTO dto = servicio.cambiarRol(6L,
            new CambiarRolRequestDTO(RolCuenta.CLIENTE_ADMINISTRADOR, 0, 3L));

        assertThat(dto.empresaId()).isEqualTo(3L);
        assertThat(dto.rol()).isEqualTo(RolCuenta.CLIENTE_ADMINISTRADOR);
    }

    @Test
    void cambiarRol_noSePuedeQuitarElRolAlUltimoAdministradorActivo() {
        Cuenta ultimo = cuenta(6L, RolCuenta.ADMINISTRADOR, null, EstadoCuenta.ACTIVA, 0);
        when(cuentaRepository.findById(6L)).thenReturn(Optional.of(ultimo));
        when(cuentaRepository.countByRolAndEstadoAndEmpresaIdIsNull(RolCuenta.ADMINISTRADOR, EstadoCuenta.ACTIVA))
            .thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarRol(6L,
            new CambiarRolRequestDTO(RolCuenta.CLIENTE_ADMINISTRADOR, 0, 3L)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    @Test
    void cambiarRol_noPermiteCambiarElPropioRol() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> servicio.cambiarRol(1L,
            new CambiarRolRequestDTO(RolCuenta.CLIENTE_ADMINISTRADOR, 3, 3L)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    @Test
    void cambiarRol_conVersionVieja_responde409() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.ACTIVA, 4);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        assertThatThrownBy(() -> servicio.cambiarRol(5L,
            new CambiarRolRequestDTO(RolCuenta.CLIENTE_ADMINISTRADOR, 3, null)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    @Test
    void cambiarRol_alMismoRol_responde400() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.ACTIVA, 4);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        assertThatThrownBy(() -> servicio.cambiarRol(5L,
            new CambiarRolRequestDTO(RolCuenta.REPRESENTANTE, 4, null)))
            .satisfies(e -> assertEstado(e, HttpStatus.BAD_REQUEST));
    }

    // ---- cambiar estado ----

    @Test
    void cambiarEstado_desactivar_dejaLaCuentaInactivaYNoUsaLaBitacora() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.ACTIVA, 1);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        CuentaGlobalDTO dto = servicio.cambiarEstado(5L, new CambiarEstadoRequestDTO(false, 1));

        assertThat(dto.estado()).isEqualTo(EstadoCuenta.INACTIVA);
        verify(cambioRolRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_reactivarQuienNuncaIngreso_vuelveAPendiente() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.INACTIVA, 1);
        objetivo.setUltimoIngresoEn(null);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        assertThat(servicio.cambiarEstado(5L, new CambiarEstadoRequestDTO(true, 1)).estado())
            .isEqualTo(EstadoCuenta.PENDIENTE);
    }

    @Test
    void cambiarEstado_reactivarQuienYaIngreso_vuelveAActiva() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.INACTIVA, 1);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        assertThat(servicio.cambiarEstado(5L, new CambiarEstadoRequestDTO(true, 1)).estado())
            .isEqualTo(EstadoCuenta.ACTIVA);
    }

    @Test
    void cambiarEstado_noPermiteDesactivarLaPropiaCuenta() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> servicio.cambiarEstado(1L, new CambiarEstadoRequestDTO(false, 3)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    @Test
    void cambiarEstado_noPermiteDesactivarAlUltimoAdministradorActivo() {
        Cuenta ultimo = cuenta(6L, RolCuenta.ADMINISTRADOR, null, EstadoCuenta.ACTIVA, 0);
        when(cuentaRepository.findById(6L)).thenReturn(Optional.of(ultimo));
        when(cuentaRepository.countByRolAndEstadoAndEmpresaIdIsNull(RolCuenta.ADMINISTRADOR, EstadoCuenta.ACTIVA))
            .thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarEstado(6L, new CambiarEstadoRequestDTO(false, 0)))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
    }

    // ---- reenviar contraseña temporal ----

    @Test
    void reenviarContrasenaTemporal_cuentaInactiva_responde409() {
        when(cuentaRepository.findById(5L))
            .thenReturn(Optional.of(cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.INACTIVA, 1)));

        assertThatThrownBy(() -> servicio.reenviarContrasenaTemporal(5L))
            .satisfies(e -> assertEstado(e, HttpStatus.CONFLICT));
        verify(correoService, never()).enviarContrasenaTemporal(anyString(), anyString(), anyString(), any());
    }

    @Test
    void reenviarContrasenaTemporal_generaUnaNuevaYLaEnvia() {
        Cuenta objetivo = cuenta(5L, RolCuenta.REPRESENTANTE, 2L, EstadoCuenta.PENDIENTE, 1);
        when(cuentaRepository.findById(5L)).thenReturn(Optional.of(objetivo));

        CuentaGlobalDTO dto = servicio.reenviarContrasenaTemporal(5L);

        assertThat(dto.correoEnviado()).isTrue();
        assertThat(dto.contrasenaTemporalPendiente()).isTrue();
        assertThat(objetivo.getContrasenaHash()).isNotBlank();
        verify(correoService).enviarContrasenaTemporal(anyString(), anyString(), anyString(), any());
    }
}

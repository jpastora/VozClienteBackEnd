package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/** Pruebas de la empresa efectiva (modo soporte del administrador de la plataforma). */
@ExtendWith(MockitoExtension.class)
class UsuarioActualServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private UsuarioActualService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioActualService(cuentaRepository, empresaRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void iniciarSesion(Long id, RolCuenta rol, Long empresaId) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(id);
        cuenta.setRol(rol);
        cuenta.setEmpresaId(empresaId);
        lenient().when(cuentaRepository.findById(id)).thenReturn(Optional.of(cuenta));

        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        var principal = new DefaultOAuth2User(autoridades,
            Map.of(UsuarioActualService.ATRIBUTO_CUENTA_ID, id, "email", "x@y.com"), "email");
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, autoridades));
        SecurityContextHolder.setContext(contexto);
    }

    private void cabeceraEmpresa(String valor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (valor != null) {
            request.addHeader(UsuarioActualService.CABECERA_EMPRESA, valor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void empresaEfectiva_clienteAdministrador_usaLaDeSuCuentaAunqueVengaLaCabecera() {
        iniciarSesion(1L, RolCuenta.CLIENTE_ADMINISTRADOR, 7L);
        cabeceraEmpresa("99");

        assertThat(servicio.empresaEfectiva()).isEqualTo(7L);
    }

    @Test
    void empresaEfectiva_administrador_usaLaCabeceraSiLaEmpresaExiste() {
        iniciarSesion(1L, RolCuenta.ADMINISTRADOR, null);
        cabeceraEmpresa("2");
        lenient().when(empresaRepository.existsById(2L)).thenReturn(true);

        assertThat(servicio.empresaEfectiva()).isEqualTo(2L);
    }

    @Test
    void empresaEfectiva_administradorSinCabecera_responde400() {
        iniciarSesion(1L, RolCuenta.ADMINISTRADOR, null);
        cabeceraEmpresa(null);

        assertThatThrownBy(() -> servicio.empresaEfectiva())
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void empresaEfectiva_administradorConCabeceraNoNumerica_responde400() {
        iniciarSesion(1L, RolCuenta.ADMINISTRADOR, null);
        cabeceraEmpresa("abc");

        assertThatThrownBy(() -> servicio.empresaEfectiva())
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void empresaEfectiva_administradorConEmpresaInexistente_responde404() {
        iniciarSesion(1L, RolCuenta.ADMINISTRADOR, null);
        cabeceraEmpresa("55");
        lenient().when(empresaRepository.existsById(55L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.empresaEfectiva())
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void empresaEfectiva_cuentaNoAdministradoraSinEmpresa_responde403() {
        iniciarSesion(1L, RolCuenta.REPRESENTANTE, null);

        assertThatThrownBy(() -> servicio.empresaEfectiva())
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void puedeAccederAEmpresa_administradorEnCualquiera_yElRestoSoloEnLaSuya() {
        iniciarSesion(1L, RolCuenta.ADMINISTRADOR, null);
        assertThat(servicio.puedeAccederAEmpresa(42L)).isTrue();

        iniciarSesion(2L, RolCuenta.CLIENTE_ADMINISTRADOR, 7L);
        assertThat(servicio.puedeAccederAEmpresa(7L)).isTrue();
        assertThat(servicio.puedeAccederAEmpresa(8L)).isFalse();
        assertThat(servicio.puedeAccederAEmpresa(null)).isFalse();
    }
}

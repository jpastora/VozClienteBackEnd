package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.dto.PagoPaqueteDTO;
import com.eduardo.vozclientebackend.dto.SaldoSalasDTO;
import com.eduardo.vozclientebackend.enums.EstadoPago;
import com.eduardo.vozclientebackend.mappers.CompraMapper;
import com.eduardo.vozclientebackend.models.Compra;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.PaqueteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la compra de paquetes con pago simulado (RF-18).
 */
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    private static final Long EMPRESA = 3L;
    private static final UUID CLAVE = UUID.fromString("7a1b2c3d-0000-4000-8000-000000000000");

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PaqueteRepository paqueteRepository;

    @Mock
    private CompraMapper compraMapper;

    @Mock
    private SaldoSalasService saldoSalasService;

    private CompraService servicio;
    private Cuenta cuenta;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC);
        servicio = new CompraService(compraRepository, cuentaRepository, empresaRepository, paqueteRepository,
            compraMapper, new PagoSimuladoService(reloj), saldoSalasService, reloj);

        empresa = new Empresa();
        empresa.setId(EMPRESA);
        cuenta = new Cuenta();
        cuenta.setId(5L);
        cuenta.setEmpresaId(EMPRESA);

        lenient().when(compraMapper.toDto(any(Compra.class))).thenAnswer(i -> {
            Compra c = i.getArgument(0);
            return new CompraDTO(c.getId(), c.getEmpresa().getId(), c.getPaquete().getId(),
                c.getCuenta() == null ? null : c.getCuenta().getId(), c.getMonto(), c.getSalasAcreditadas(),
                c.getEstadoPago(), c.getReferenciaPago(), c.getClaveIdempotencia(), c.getCreadoEn());
        });
    }

    private static Paquete paquete(boolean activo) {
        Paquete p = new Paquete();
        p.setId(2L);
        p.setNombre("Intermedio");
        p.setCantidadSalas(50);
        p.setPrecio(new BigDecimal("600.00"));
        p.setEsActivo(activo);
        return p;
    }

    private static PagoPaqueteDTO pago(String tarjeta) {
        return new PagoPaqueteDTO(2L, CLAVE, "Ana Vargas", tarjeta, "12/28", "123");
    }

    private static int estadoDe(Throwable e) {
        return ((ResponseStatusException) e).getStatusCode().value();
    }

    private void empresaYPaquete(Paquete p) {
        when(empresaRepository.bloquearEmpresa(EMPRESA)).thenReturn(Optional.of(empresa));
        when(compraRepository.findByClaveIdempotencia(CLAVE)).thenReturn(Optional.empty());
        when(paqueteRepository.findById(2L)).thenReturn(Optional.of(p));
    }

    @Test
    void comprar_tarjetaValida_registraAprobadaConMontoYSalasDelPaquete() {
        empresaYPaquete(paquete(true));
        when(cuentaRepository.getReferenceById(5L)).thenReturn(cuenta);
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        CompraDTO compra = servicio.comprarPaquete(pago("4242424242424242"), cuenta);

        assertThat(compra.estadoPago()).isEqualTo(EstadoPago.APROBADO);
        assertThat(compra.empresaId()).isEqualTo(EMPRESA);
        assertThat(compra.monto()).isEqualByComparingTo("600.00");
        assertThat(compra.salasAcreditadas()).isEqualTo(50);
        assertThat(compra.referenciaPago()).endsWith("****4242");
        assertThat(compra.claveIdempotencia()).isEqualTo(CLAVE);
    }

    @Test
    void comprar_tarjetaRechazada_registraLaCompraComoRechazada() {
        empresaYPaquete(paquete(true));
        when(cuentaRepository.getReferenceById(5L)).thenReturn(cuenta);
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        CompraDTO compra = servicio.comprarPaquete(pago("4000000000000002"), cuenta);

        assertThat(compra.estadoPago()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(compra.referenciaPago()).contains("rechazada");
    }

    @Test
    void comprar_mismaClave_devuelveLaCompraExistenteSinCobrarDeNuevo() {
        Compra previa = new Compra();
        previa.setId(9L);
        previa.setEmpresa(empresa);
        previa.setPaquete(paquete(true));
        previa.setEstadoPago(EstadoPago.APROBADO);
        previa.setClaveIdempotencia(CLAVE);
        when(empresaRepository.bloquearEmpresa(EMPRESA)).thenReturn(Optional.of(empresa));
        when(compraRepository.findByClaveIdempotencia(CLAVE)).thenReturn(Optional.of(previa));

        CompraDTO compra = servicio.comprarPaquete(pago("4242424242424242"), cuenta);

        assertThat(compra.id()).isEqualTo(9L);
        verify(compraRepository, never()).save(any());
    }

    @Test
    void comprar_paqueteInactivo_responde409SinRegistrar() {
        empresaYPaquete(paquete(false));

        assertThatThrownBy(() -> servicio.comprarPaquete(pago("4242424242424242"), cuenta))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void comprar_tarjetaInvalida_responde400SinRegistrar() {
        empresaYPaquete(paquete(true));

        assertThatThrownBy(() -> servicio.comprarPaquete(pago("1234"), cuenta))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void comprar_cuentaSinEmpresa_responde403() {
        cuenta.setEmpresaId(null);

        assertThatThrownBy(() -> servicio.comprarPaquete(pago("4242424242424242"), cuenta))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void comprar_sinClaveDeIdempotencia_responde400() {
        PagoPaqueteDTO sinClave = new PagoPaqueteDTO(2L, null, "Ana Vargas", "4242424242424242", "12/28", "123");

        assertThatThrownBy(() -> servicio.comprarPaquete(sinClave, cuenta))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    private Compra compraAprobada(int salas) {
        Compra c = new Compra();
        c.setId(9L);
        c.setEmpresa(empresa);
        c.setSalasAcreditadas(salas);
        c.setEstadoPago(EstadoPago.APROBADO);
        return c;
    }

    @Test
    void eliminar_compraYaUsada_responde409() {
        Compra compra = compraAprobada(10);
        when(compraRepository.findById(9L)).thenReturn(Optional.of(compra));
        when(saldoSalasService.obtener(EMPRESA)).thenReturn(new SaldoSalasDTO(EMPRESA, 10, 2, 1, 8, 7));

        assertThatThrownBy(() -> servicio.eliminarCompra(9L))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(compraRepository, never()).delete(any());
    }

    @Test
    void eliminar_compraSinUsar_seElimina() {
        Compra compra = compraAprobada(10);
        when(compraRepository.findById(9L)).thenReturn(Optional.of(compra));
        when(saldoSalasService.obtener(EMPRESA)).thenReturn(new SaldoSalasDTO(EMPRESA, 60, 20, 5, 40, 35));

        servicio.eliminarCompra(9L);

        verify(compraRepository).delete(compra);
    }
}

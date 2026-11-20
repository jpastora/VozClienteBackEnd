package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.SaldoSalasDTO;
import com.eduardo.vozclientebackend.enums.EstadoPago;
import com.eduardo.vozclientebackend.enums.EstadoSala;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Pruebas del saldo de salas (RF-07, RF-18).
 */
@ExtendWith(MockitoExtension.class)
class SaldoSalasServiceTest {

    private static final Long EMPRESA = 3L;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private SalaRepository salaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    private SaldoSalasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new SaldoSalasService(compraRepository, salaRepository, empresaRepository,
            Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC));
    }

    private void datos(long acreditadas, long consumidas, long reservadas) {
        when(compraRepository.sumarSalasPorEstado(EMPRESA, EstadoPago.APROBADO)).thenReturn(acreditadas);
        when(salaRepository.contarConsumidasPorEmpresa(eq(EMPRESA), any())).thenReturn(consumidas);
        when(salaRepository.contarReservadasPorEmpresa(eq(EMPRESA), any(), eq(EstadoSala.EXPIRADO), any()))
            .thenReturn(reservadas);
    }

    @Test
    void obtener_calculaSaldoYDisponibles() {
        datos(10, 3, 2);

        SaldoSalasDTO saldo = servicio.obtener(EMPRESA);

        assertThat(saldo.acreditadas()).isEqualTo(10);
        assertThat(saldo.consumidas()).isEqualTo(3);
        assertThat(saldo.reservadas()).isEqualTo(2);
        assertThat(saldo.saldo()).isEqualTo(7);
        assertThat(saldo.disponibles()).isEqualTo(5);
    }

    @Test
    void obtener_sinCompras_saldoCero() {
        when(compraRepository.sumarSalasPorEstado(EMPRESA, EstadoPago.APROBADO)).thenReturn(null);
        when(salaRepository.contarConsumidasPorEmpresa(eq(EMPRESA), any())).thenReturn(0L);
        when(salaRepository.contarReservadasPorEmpresa(eq(EMPRESA), any(), eq(EstadoSala.EXPIRADO), any()))
            .thenReturn(0L);

        SaldoSalasDTO saldo = servicio.obtener(EMPRESA);

        assertThat(saldo.saldo()).isZero();
        assertThat(saldo.disponibles()).isZero();
    }

    @Test
    void obtener_elSaldoNuncaEsNegativo() {
        datos(2, 5, 1);

        SaldoSalasDTO saldo = servicio.obtener(EMPRESA);

        assertThat(saldo.saldo()).isZero();
        assertThat(saldo.disponibles()).isZero();
    }

    @Test
    void exigirSalaDisponible_conSaldo_noLanza() {
        when(empresaRepository.bloquearEmpresa(EMPRESA)).thenReturn(Optional.of(new Empresa()));
        datos(10, 3, 6);

        assertThatCode(() -> servicio.exigirSalaDisponible(EMPRESA)).doesNotThrowAnyException();
    }

    @Test
    void exigirSalaDisponible_todasReservadas_responde409() {
        when(empresaRepository.bloquearEmpresa(EMPRESA)).thenReturn(Optional.of(new Empresa()));
        datos(10, 3, 7);

        assertThatThrownBy(() -> servicio.exigirSalaDisponible(EMPRESA))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.CONFLICT.value()))
            .hasMessageContaining("no tiene salas disponibles");
    }

    @Test
    void exigirSalaDisponible_empresaInexistente_responde404() {
        when(empresaRepository.bloquearEmpresa(EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.exigirSalaDisponible(EMPRESA))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }
}

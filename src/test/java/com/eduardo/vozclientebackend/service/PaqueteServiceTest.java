package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.PaqueteDTO;
import com.eduardo.vozclientebackend.mappers.PaqueteMapper;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.CompraRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de los paquetes de salas (RF-17).
 */
@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    @Mock
    private PaqueteRepository paqueteRepository;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private PaqueteMapper paqueteMapper;

    private PaqueteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PaqueteService(paqueteRepository, compraRepository, paqueteMapper,
            Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC));
        lenient().when(paqueteMapper.toDto(any(Paquete.class))).thenAnswer(i -> {
            Paquete p = i.getArgument(0);
            return new PaqueteDTO(p.getId(), p.getNombre(), p.getCantidadSalas(), p.getPrecio(), p.getEsActivo(),
                p.getCreadoEn(), p.getActualizadoEn());
        });
    }

    private static PaqueteDTO datos(String nombre, Integer salas, String precio) {
        return new PaqueteDTO(null, nombre, salas, precio == null ? null : new BigDecimal(precio), null, null, null);
    }

    private static Paquete existente(Long id, String nombre) {
        Paquete p = new Paquete();
        p.setId(id);
        p.setNombre(nombre);
        p.setCantidadSalas(10);
        p.setPrecio(new BigDecimal("150.00"));
        p.setEsActivo(true);
        return p;
    }

    private static int estadoDe(Throwable e) {
        return ((ResponseStatusException) e).getStatusCode().value();
    }

    @Test
    void crear_datosValidos_quedaActivoConElNombreLimpio() {
        when(paqueteRepository.findByNombreIgnoreCase("Plan Básico")).thenReturn(Optional.empty());
        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(i -> i.getArgument(0));

        PaqueteDTO creado = servicio.crearPaquete(datos("  Plan   Básico ", 10, "150.00"));

        assertThat(creado.nombre()).isEqualTo("Plan Básico");
        assertThat(creado.esActivo()).isTrue();
        assertThat(creado.creadoEn()).isNotNull();
    }

    @Test
    void crear_nombreDuplicadoSinImportarMayusculas_responde409() {
        when(paqueteRepository.findByNombreIgnoreCase("plan básico"))
            .thenReturn(Optional.of(existente(1L, "Plan Básico")));

        assertThatThrownBy(() -> servicio.crearPaquete(datos("plan básico", 10, "150")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(paqueteRepository, never()).save(any());
    }

    @Test
    void crear_nombreVacio_responde400() {
        assertThatThrownBy(() -> servicio.crearPaquete(datos("   ", 10, "150")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void crear_cantidadDeSalasCero_responde400() {
        assertThatThrownBy(() -> servicio.crearPaquete(datos("Plan", 0, "150")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void crear_precioNegativo_responde400() {
        assertThatThrownBy(() -> servicio.crearPaquete(datos("Plan", 10, "-1")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void crear_precioConTresDecimales_responde400() {
        assertThatThrownBy(() -> servicio.crearPaquete(datos("Plan", 10, "10.999")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.BAD_REQUEST.value()))
            .hasMessageContaining("dos decimales");
    }

    @Test
    void crear_precioCero_esValido() {
        when(paqueteRepository.findByNombreIgnoreCase("Prueba")).thenReturn(Optional.empty());
        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(servicio.crearPaquete(datos("Prueba", 1, "0")).precio()).isEqualByComparingTo("0");
    }

    @Test
    void actualizar_conservandoSuPropioNombre_esValido() {
        Paquete paquete = existente(1L, "Plan Básico");
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(paquete));
        when(paqueteRepository.findByNombreIgnoreCase("Plan Básico")).thenReturn(Optional.of(paquete));
        when(paqueteRepository.save(any(Paquete.class))).thenAnswer(i -> i.getArgument(0));

        PaqueteDTO actualizado = servicio.actualizarPaquete(1L, datos("Plan Básico", 12, "180.50"));

        assertThat(actualizado.cantidadSalas()).isEqualTo(12);
        assertThat(actualizado.precio()).isEqualByComparingTo("180.50");
    }

    @Test
    void actualizar_conNombreDeOtroPaquete_responde409() {
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(existente(1L, "Plan Básico")));
        when(paqueteRepository.findByNombreIgnoreCase("Premium")).thenReturn(Optional.of(existente(3L, "Premium")));

        assertThatThrownBy(() -> servicio.actualizarPaquete(1L, datos("Premium", 10, "150")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT.value()));
    }

    @Test
    void actualizar_paqueteInexistente_responde404() {
        when(paqueteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizarPaquete(99L, datos("Plan", 10, "150")))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void eliminar_paqueteConCompras_responde409() {
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(existente(1L, "Plan Básico")));
        when(compraRepository.existsByPaqueteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminarPaquete(1L))
            .satisfies(e -> assertThat(estadoDe(e)).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(paqueteRepository, never()).delete(any());
    }

    @Test
    void eliminar_paqueteSinCompras_seElimina() {
        Paquete paquete = existente(1L, "Plan Básico");
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(paquete));
        when(compraRepository.existsByPaqueteId(1L)).thenReturn(false);

        servicio.eliminarPaquete(1L);

        verify(paqueteRepository).delete(paquete);
    }

    @Test
    void desactivar_dejaDeOfrecerseSinBorrarlo() {
        Paquete paquete = existente(1L, "Plan Básico");
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(paquete));

        servicio.desactivarPaquete(1L);

        assertThat(paquete.getEsActivo()).isFalse();
        verify(paqueteRepository).save(paquete);
        verify(paqueteRepository, never()).delete(any());
    }
}

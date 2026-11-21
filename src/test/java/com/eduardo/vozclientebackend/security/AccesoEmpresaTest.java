package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.models.Entrevista;
import com.eduardo.vozclientebackend.models.Grabacion;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.models.Sala;
import com.eduardo.vozclientebackend.models.Transcripcion;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EntrevistaRepository;
import com.eduardo.vozclientebackend.repository.GrabacionRepository;
import com.eduardo.vozclientebackend.repository.GuiaRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;
import com.eduardo.vozclientebackend.repository.TranscripcionRepository;
import com.eduardo.vozclientebackend.service.UsuarioActualService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

/** Pruebas del control de acceso entre empresas (cadena transcripción → … → producto → empresa). */
@ExtendWith(MockitoExtension.class)
class AccesoEmpresaTest {

    private static final long MI_EMPRESA = 1L;
    private static final long OTRA_EMPRESA = 2L;

    @Mock
    private UsuarioActualService usuarioActualService;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private SalaRepository salaRepository;
    @Mock
    private EntrevistaRepository entrevistaRepository;
    @Mock
    private GrabacionRepository grabacionRepository;
    @Mock
    private TranscripcionRepository transcripcionRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private GuiaRepository guiaRepository;

    private AccesoEmpresa acceso;

    @BeforeEach
    void preparar() {
        acceso = new AccesoEmpresa(usuarioActualService, productoRepository, salaRepository, entrevistaRepository,
            grabacionRepository, transcripcionRepository, cuentaRepository, guiaRepository);
        // Quien pide es un cliente-administrador de la empresa 1.
        lenient().when(usuarioActualService.puedeAccederAEmpresa(MI_EMPRESA)).thenReturn(true);
        lenient().when(usuarioActualService.puedeAccederAEmpresa(OTRA_EMPRESA)).thenReturn(false);
    }

    private void cadena(long empresaId) {
        Producto producto = new Producto();
        producto.setEmpresaId(empresaId);
        Sala sala = new Sala();
        sala.setProductoId(10L);
        Entrevista entrevista = new Entrevista();
        entrevista.setSalaId(20L);
        Grabacion grabacion = new Grabacion();
        grabacion.setEntrevistaId(30L);
        Transcripcion transcripcion = new Transcripcion();
        transcripcion.setGrabacionId(40L);
        lenient().when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        lenient().when(salaRepository.findById(20L)).thenReturn(Optional.of(sala));
        lenient().when(entrevistaRepository.findById(30L)).thenReturn(Optional.of(entrevista));
        lenient().when(grabacionRepository.findById(40L)).thenReturn(Optional.of(grabacion));
        lenient().when(transcripcionRepository.findById(50L)).thenReturn(Optional.of(transcripcion));
    }

    @Test
    void recursosDeLaPropiaEmpresa_seAceptan() {
        cadena(MI_EMPRESA);

        assertThatCode(() -> {
            acceso.exigirProducto(10L);
            acceso.exigirSala(20L);
            acceso.exigirEntrevista(30L);
            acceso.exigirGrabacion(40L);
            acceso.exigirTranscripcion(50L);
        }).doesNotThrowAnyException();
    }

    @Test
    void recursosDeOtraEmpresa_respondenComoSiNoExistieran() {
        cadena(OTRA_EMPRESA);

        List<Runnable> accesos = List.of(
            () -> acceso.exigirProducto(10L),
            () -> acceso.exigirSala(20L),
            () -> acceso.exigirEntrevista(30L),
            () -> acceso.exigirGrabacion(40L),
            () -> acceso.exigirTranscripcion(50L),
            () -> acceso.exigirEmpresa(OTRA_EMPRESA));
        for (Runnable a : accesos) {
            assertThatThrownBy(a::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void recursoInexistente_noSeRechazaAqui_loResuelveElServicio() {
        lenient().when(entrevistaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatCode(() -> acceso.exigirEntrevista(99L)).doesNotThrowAnyException();
        assertThatCode(() -> acceso.exigirEntrevista(null)).doesNotThrowAnyException();
    }

    @Test
    void filtrar_dejaSoloLosElementosDeEmpresasAccesibles() {
        List<Long> empresas = List.of(MI_EMPRESA, OTRA_EMPRESA, MI_EMPRESA);

        assertThat(acceso.filtrar(empresas, e -> e)).containsExactly(MI_EMPRESA, MI_EMPRESA);
    }
}

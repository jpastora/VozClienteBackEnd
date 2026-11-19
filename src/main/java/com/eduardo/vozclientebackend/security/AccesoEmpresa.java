package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Entrevista;
import com.eduardo.vozclientebackend.models.Grabacion;
import com.eduardo.vozclientebackend.models.Guia;
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

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Verifica que lo que se pide pertenezca a la empresa de quien lo pide.
 *
 * <p>El administrador de la plataforma accede a cualquier empresa; el resto, solo a la suya. Si el
 * recurso es de otra empresa se responde 404 (igual que si no existiera) para no revelar que existe.
 * Si el recurso no existe, no se rechaza aquí: el servicio responde su propio error.</p>
 *
 * <p>Se usa en los controladores y no en los servicios porque algunos servicios también los llama el
 * sistema (tareas programadas, transcripción) sin una persona autenticada.</p>
 */
@Component
@RequiredArgsConstructor
public class AccesoEmpresa {

    private final UsuarioActualService usuarioActualService;
    private final ProductoRepository productoRepository;
    private final SalaRepository salaRepository;
    private final EntrevistaRepository entrevistaRepository;
    private final GrabacionRepository grabacionRepository;
    private final TranscripcionRepository transcripcionRepository;
    private final CuentaRepository cuentaRepository;
    private final GuiaRepository guiaRepository;

    /** La empresa debe ser accesible para quien pide. */
    public void exigirEmpresa(Long empresaId) {
        if (!usuarioActualService.puedeAccederAEmpresa(empresaId)) {
            throw noEncontrado();
        }
    }

    /** Deja solo los elementos de empresas accesibles (para listados que traen todas las empresas). */
    public <T> List<T> filtrar(List<T> elementos, Function<T, Long> empresaDe) {
        return elementos.stream()
            .filter(e -> usuarioActualService.puedeAccederAEmpresa(empresaDe.apply(e)))
            .toList();
    }

    /** El id debe ser el de la cuenta con sesión (nadie actúa a nombre de otra cuenta). */
    public void exigirCuentaActual(Long cuentaId) {
        if (cuentaId != null && !Objects.equals(cuentaId, usuarioActualService.obtenerCuentaActual().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes actuar a nombre de otra cuenta");
        }
    }

    @Transactional(readOnly = true)
    public void exigirCuenta(Long cuentaId) {
        if (cuentaId == null) {
            return;
        }
        cuentaRepository.findById(cuentaId).ifPresent(c -> exigirEmpresa(c.getEmpresaId()));
    }

    @Transactional(readOnly = true)
    public void exigirProducto(Long productoId) {
        if (productoId == null) {
            return;
        }
        productoRepository.findById(productoId).ifPresent(p -> exigirEmpresa(p.getEmpresaId()));
    }

    @Transactional(readOnly = true)
    public void exigirSala(Long salaId) {
        if (salaId == null) {
            return;
        }
        salaRepository.findById(salaId).ifPresent(s -> exigirProducto(s.getProductoId()));
    }

    @Transactional(readOnly = true)
    public void exigirSalaPorToken(String token) {
        if (token == null) {
            return;
        }
        salaRepository.findByToken(token).ifPresent(s -> exigirProducto(s.getProductoId()));
    }

    @Transactional(readOnly = true)
    public void exigirEntrevista(Long entrevistaId) {
        if (entrevistaId == null) {
            return;
        }
        entrevistaRepository.findById(entrevistaId).ifPresent(e -> exigirSala(e.getSalaId()));
    }

    @Transactional(readOnly = true)
    public void exigirGrabacion(Long grabacionId) {
        if (grabacionId == null) {
            return;
        }
        grabacionRepository.findById(grabacionId).ifPresent(g -> exigirEntrevista(g.getEntrevistaId()));
    }

    @Transactional(readOnly = true)
    public void exigirTranscripcion(Long transcripcionId) {
        if (transcripcionId == null) {
            return;
        }
        transcripcionRepository.findById(transcripcionId).ifPresent(t -> exigirGrabacion(t.getGrabacionId()));
    }

    @Transactional(readOnly = true)
    public void exigirGuia(Long guiaId) {
        if (guiaId == null) {
            return;
        }
        guiaRepository.findById(guiaId).ifPresent(g -> exigirEmpresa(g.getEmpresa().getId()));
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el recurso solicitado");
    }
}

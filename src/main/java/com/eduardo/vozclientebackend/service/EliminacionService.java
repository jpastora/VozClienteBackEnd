package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Empresa;
import com.eduardo.vozclientebackend.models.Entrevista;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.repository.CambioRolRepository;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
import com.eduardo.vozclientebackend.repository.EntrevistaRepository;
import com.eduardo.vozclientebackend.repository.GrabacionRepository;
import com.eduardo.vozclientebackend.repository.GuiaRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Eliminaciones con validación de integridad: nada se borra en cascada. Si el registro tiene datos
 * relacionados responde 409 con el detalle y el orden en que conviene depurar (de las hojas a la
 * raíz): entrevistas → salas → productos → representantes/usuarios → empresa.
 */
@Service
@RequiredArgsConstructor
public class EliminacionService {

    private final EmpresaRepository empresaRepository;
    private final CuentaRepository cuentaRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final GuiaRepository guiaRepository;
    private final SalaRepository salaRepository;
    private final EntrevistaRepository entrevistaRepository;
    private final GrabacionRepository grabacionRepository;
    private final CambioRolRepository cambioRolRepository;
    private final UsuarioActualService usuarioActualService;

    /** Detalle de lo que impide eliminar, armado con "N cosas y M cosas". */
    private static final class Bloqueos {
        private final List<String> partes = new ArrayList<>();

        Bloqueos agregar(long cantidad, String singular, String plural) {
            if (cantidad > 0) {
                partes.add(cantidad + " " + (cantidad == 1 ? singular : plural));
            }
            return this;
        }

        void exigirVacio(String sujeto, String orden) {
            if (partes.isEmpty()) {
                return;
            }
            String detalle = partes.size() == 1
                ? partes.get(0)
                : String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar " + sujeto + " porque tiene " + detalle + ". " + orden);
        }
    }

    private static final String ORDEN_EMPRESA =
        "Primero depura: entrevistas, salas, productos, representantes y usuarios; después la empresa.";
    private static final String ORDEN_CUENTA =
        "Reasigna o elimina primero esos datos, o desactiva la cuenta para conservar el historial.";

    /** Defensa en profundidad: aunque cambie la matriz de permisos, solo el administrador elimina. */
    private void exigirAdministradorPlataforma() {
        if (usuarioActualService.obtenerCuentaActual().getRol() != RolCuenta.ADMINISTRADOR) {
            throw new AccessDeniedException("Solo el administrador de la plataforma puede eliminar.");
        }
    }

    @Transactional
    public void eliminarEmpresa(Long id) {
        exigirAdministradorPlataforma();
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no existe."));

        new Bloqueos()
            .agregar(cuentaRepository.countByEmpresaId(id), "cuenta", "cuentas")
            .agregar(productoRepository.countByEmpresaId(id), "producto", "productos")
            .agregar(compraRepository.countByEmpresaId(id), "compra", "compras")
            .agregar(guiaRepository.countByEmpresaId(id), "guía", "guías")
            .exigirVacio("la empresa «" + empresa.getNombre() + "»", ORDEN_EMPRESA);

        empresaRepository.delete(empresa);
        empresaRepository.flush();
    }

    @Transactional
    public void eliminarProducto(Long id) {
        exigirAdministradorPlataforma();
        Long empresaId = usuarioActualService.empresaEfectiva();
        Producto producto = productoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto."));

        new Bloqueos()
            .agregar(salaRepository.countByProductoId(id), "sala", "salas")
            .exigirVacio("el producto «" + producto.getNombre() + "»",
                "Elimina primero sus salas (y entrevistas), o desactiva el producto para conservar el historial.");

        productoRepository.delete(producto);
        productoRepository.flush();
    }

    /** Elimina una cuenta (cualquier rol) desde la pantalla Usuarios; solo el administrador de la plataforma. */
    @Transactional
    public void eliminarCuenta(Long id) {
        exigirAdministradorPlataforma();
        Cuenta actual = usuarioActualService.obtenerCuentaActual();
        Cuenta cuenta = cuentaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no existe."));
        eliminarCuentaValidada(actual, cuenta);
    }

    /** Elimina un representante de la empresa en uso (propia, o la elegida en modo soporte). */
    @Transactional
    public void eliminarRepresentante(Long id) {
        exigirAdministradorPlataforma();
        Cuenta actual = usuarioActualService.obtenerCuentaActual();
        Long empresaId = usuarioActualService.empresaEfectiva();
        Cuenta representante = cuentaRepository.findByIdAndEmpresaIdAndRol(id, empresaId, RolCuenta.REPRESENTANTE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Representante no encontrado"));
        eliminarCuentaValidada(actual, representante);
    }

    private void eliminarCuentaValidada(Cuenta actual, Cuenta cuenta) {
        if (cuenta.getId().equals(actual.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes eliminar tu propia cuenta.");
        }
        if (cuenta.getRol() == RolCuenta.ADMINISTRADOR && cuentaRepository.findByRol(RolCuenta.ADMINISTRADOR).size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar al único administrador de la plataforma.");
        }

        Long id = cuenta.getId();
        new Bloqueos()
            .agregar(productoRepository.countByRepresentanteId(id), "producto asignado", "productos asignados")
            .agregar(entrevistaRepository.countByRepresentanteId(id), "entrevista", "entrevistas")
            .agregar(salaRepository.countByCreadoPorId(id), "sala creada", "salas creadas")
            .agregar(compraRepository.countByCuentaId(id), "compra", "compras")
            .agregar(guiaRepository.countByCuentaId(id), "guía", "guías")
            .agregar(cambioRolRepository.countByAutorIdAndCuentaIdNot(id, id), "cambio de rol hecho a otras cuentas",
                "cambios de rol hechos a otras cuentas")
            .exigirVacio("a «" + cuenta.getNombreCompleto() + "»", ORDEN_CUENTA);

        cambioRolRepository.deleteByCuentaId(id);
        cuentaRepository.delete(cuenta);
        cuentaRepository.flush();
    }

    /** Solo entrevistas fallidas o en curso y sin grabación; las finalizadas alimentan análisis y estadísticas. */
    @Transactional
    public void eliminarEntrevista(Long id) {
        exigirAdministradorPlataforma();
        Entrevista entrevista = entrevistaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La entrevista no existe."));

        if (entrevista.getEstado() == EstadoEntrevista.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar una entrevista finalizada: alimenta el análisis y las estadísticas.");
        }
        if (grabacionRepository.existsByEntrevistaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar la entrevista porque ya tiene una grabación.");
        }

        entrevistaRepository.delete(entrevista);
        entrevistaRepository.flush();
    }
}

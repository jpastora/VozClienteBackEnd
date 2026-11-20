package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.PaqueteDTO;
import com.eduardo.vozclientebackend.mappers.PaqueteMapper;
import com.eduardo.vozclientebackend.models.Paquete;
import com.eduardo.vozclientebackend.repository.CompraRepository;
import com.eduardo.vozclientebackend.repository.PaqueteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Paquetes de salas (RF-17). Los gestiona el administrador de la plataforma.
 *
 * <ul>
 *   <li>Nombre obligatorio, de hasta 50 caracteres y único (sin importar mayúsculas).</li>
 *   <li>Cantidad de salas: entero mayor o igual que 1. Precio: mayor o igual que 0, con dos decimales
 *       como máximo.</li>
 *   <li>Editar un paquete solo afecta las compras futuras: cada compra guarda su monto y sus salas.</li>
 *   <li>Desactivar un paquete deja de ofrecerlo, pero conserva el saldo de quienes ya lo compraron.
 *       Un paquete que ya se vendió no se elimina: se desactiva.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PaqueteService {

    private static final int LARGO_MAXIMO_NOMBRE = 50;
    private static final int DECIMALES_PRECIO = 2;

    private final PaqueteRepository paqueteRepository;
    private final CompraRepository compraRepository;
    private final PaqueteMapper paqueteMapper;
    private final Clock reloj;

    /**
     * Crea un paquete; si no se indica el estado, queda activo.
     *
     * @throws ResponseStatusException 400 si los datos no son válidos; 409 si el nombre ya existe
     */
    @Transactional
    public PaqueteDTO crearPaquete(PaqueteDTO datos) {
        String nombre = validar(datos);
        exigirNombreLibre(nombre, null);

        Paquete paquete = new Paquete();
        paquete.setNombre(nombre);
        paquete.setCantidadSalas(datos.cantidadSalas());
        paquete.setPrecio(datos.precio());
        paquete.setEsActivo(datos.esActivo() == null || datos.esActivo());
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        paquete.setCreadoEn(ahora);
        paquete.setActualizadoEn(ahora);
        return paqueteMapper.toDto(paqueteRepository.save(paquete));
    }

    /**
     * Actualiza un paquete. Las compras ya hechas conservan su monto y sus salas.
     *
     * @throws ResponseStatusException 400 si los datos no son válidos; 404 si no existe; 409 si el
     *                                 nombre lo usa otro paquete
     */
    @Transactional
    public PaqueteDTO actualizarPaquete(Long id, PaqueteDTO datos) {
        Paquete paquete = buscar(id);
        String nombre = validar(datos);
        exigirNombreLibre(nombre, id);

        paquete.setNombre(nombre);
        paquete.setCantidadSalas(datos.cantidadSalas());
        paquete.setPrecio(datos.precio());
        if (datos.esActivo() != null) {
            paquete.setEsActivo(datos.esActivo());
        }
        paquete.setActualizadoEn(OffsetDateTime.now(reloj));
        return paqueteMapper.toDto(paqueteRepository.save(paquete));
    }

    /**
     * Obtiene todos los paquetes (activos e inactivos).
     */
    @Transactional(readOnly = true)
    public List<PaqueteDTO> obtenerTodos() {
        return paqueteMapper.toDtoList(paqueteRepository.findAll());
    }

    /**
     * Obtiene únicamente los paquetes activos (catálogo para el cliente-administrador).
     */
    @Transactional(readOnly = true)
    public List<PaqueteDTO> obtenerActivos() {
        return paqueteMapper.toDtoList(paqueteRepository.findByEsActivoTrue());
    }

    /**
     * Obtiene un paquete por su ID.
     */
    @Transactional(readOnly = true)
    public PaqueteDTO obtenerPorId(Long id) {
        return paqueteMapper.toDto(buscar(id));
    }

    /**
     * Obtiene un paquete por su nombre exacto.
     */
    @Transactional(readOnly = true)
    public PaqueteDTO obtenerPorNombre(String nombre) {
        Paquete paquete = paqueteRepository.findByNombre(nombre)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el paquete"));
        return paqueteMapper.toDto(paquete);
    }

    /**
     * Elimina un paquete que nunca se vendió.
     *
     * @throws ResponseStatusException 404 si no existe; 409 si ya tiene compras
     */
    @Transactional
    public void eliminarPaquete(Long id) {
        Paquete paquete = buscar(id);
        if (compraRepository.existsByPaqueteId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Este paquete ya tiene compras. Desactívalo para dejar de ofrecerlo y conservar el historial.");
        }
        paqueteRepository.delete(paquete);
    }

    /**
     * Desactiva un paquete: deja de ofrecerse, pero las empresas que lo compraron conservan su saldo.
     */
    @Transactional
    public void desactivarPaquete(Long id) {
        cambiarEstado(id, false);
    }

    /**
     * Vuelve a ofrecer un paquete desactivado.
     */
    @Transactional
    public void activarPaquete(Long id) {
        cambiarEstado(id, true);
    }

    private void cambiarEstado(Long id, boolean activo) {
        Paquete paquete = buscar(id);
        paquete.setEsActivo(activo);
        paquete.setActualizadoEn(OffsetDateTime.now(reloj));
        paqueteRepository.save(paquete);
    }

    private Paquete buscar(Long id) {
        return paqueteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el paquete"));
    }

    /** Valida los datos (RF-17) y devuelve el nombre limpio. */
    static String validar(PaqueteDTO datos) {
        if (datos == null) {
            throw invalido("Los datos del paquete son obligatorios.");
        }
        String nombre = datos.nombre() == null ? "" : datos.nombre().trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) {
            throw invalido("El nombre del paquete es obligatorio.");
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            throw invalido("El nombre del paquete admite hasta " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (datos.cantidadSalas() == null || datos.cantidadSalas() < 1) {
            throw invalido("La cantidad de salas debe ser un número entero mayor o igual que 1.");
        }
        BigDecimal precio = datos.precio();
        if (precio == null || precio.signum() < 0) {
            throw invalido("El precio debe ser un número mayor o igual que 0.");
        }
        if (precio.stripTrailingZeros().scale() > DECIMALES_PRECIO) {
            throw invalido("El precio admite como máximo dos decimales.");
        }
        return nombre;
    }

    private void exigirNombreLibre(String nombre, Long idPropio) {
        paqueteRepository.findByNombreIgnoreCase(nombre)
            .filter(otro -> !otro.getId().equals(idPropio))
            .ifPresent(otro -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un paquete con ese nombre.");
            });
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

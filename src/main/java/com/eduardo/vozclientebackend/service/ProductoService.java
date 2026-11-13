package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.exceptions.ProductoDuplicadoException;
import com.eduardo.vozclientebackend.mappers.ProductoMapper;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private static final String ROL_REPRESENTANTE = "REPRESENTANTE";

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final UsuarioActualService usuarioActualService;
    private final CuentaRepository cuentaRepository;

    private Long obtenerEmpresaId() {
        Long empresaId = usuarioActualService
            .obtenerCuentaActual()
            .getEmpresaId();

        if (empresaId == null) {
            throw new IllegalStateException(
                "El usuario no tiene una empresa asignada."
            );
        }

        return empresaId;
    }

    private boolean esRepresentante(Cuenta cuenta) {
        // String.valueOf funciona tanto si el rol es String como si es un enum.
        return ROL_REPRESENTANTE.equals(String.valueOf(cuenta.getRol()));
    }

    /**
     * Id del representante por el que se filtra: el de la sesión si quien consulta es un
     * representante, o null (sin filtro) para el resto de los roles.
     */
    private Long representanteFiltro() {
        Cuenta cuenta = usuarioActualService.obtenerCuentaActual();
        return esRepresentante(cuenta) ? cuenta.getId() : null;
    }

    /** Crear, editar, inactivar y eliminar productos no está permitido a un representante. */
    private void exigirNoRepresentante() {
        if (esRepresentante(usuarioActualService.obtenerCuentaActual())) {
            throw new AccessDeniedException(
                "Un representante no puede modificar productos."
            );
        }
    }

    /** El representante asignado debe ser una cuenta activa con rol REPRESENTANTE de la misma empresa. */
    private void validarRepresentante(Long empresaId, Long representanteId) {
        if (representanteId == null) {
            return;
        }

        Cuenta rep = cuentaRepository.findById(representanteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "El representante indicado no existe."
            ));

        boolean valido =
            Objects.equals(rep.getEmpresaId(), empresaId)
                && esRepresentante(rep)
                && !"INACTIVA".equals(String.valueOf(rep.getEstado()));

        if (!valido) {
            throw new IllegalArgumentException(
                "El representante debe ser una cuenta activa de la empresa con rol REPRESENTANTE."
            );
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private void validarNombreUnico(
        Long empresaId,
        String nombre,
        Long excluirId) {

        if (productoRepository.existeNombreEnEmpresa(
            empresaId, nombre, excluirId)) {
            throw new ProductoDuplicadoException(nombre);
        }
    }

    // HU-04: Registrar producto
    @Transactional
    public ProductoDTO crearProducto(
        ProductoRequestDTO request) {

        exigirNoRepresentante();

        Long empresaId = obtenerEmpresaId();
        String nombre = normalizarNombre(request.nombre());

        validarNombreUnico(empresaId, nombre, null);
        validarRepresentante(empresaId, request.representanteId());

        Producto producto = productoMapper.toEntity(request);

        producto.setEmpresaId(empresaId);
        producto.setNombre(nombre);
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(request.categoria());
        producto.setRepresentanteId(request.representanteId());

        producto.setEsActivo(
            request.esActivo() == null
                ? true
                : request.esActivo()
        );

        producto.setVersion(0);

        OffsetDateTime ahora = OffsetDateTime.now();
        producto.setCreadoEn(ahora);
        producto.setActualizadoEn(ahora);

        Producto guardado = productoRepository.save(producto);

        return productoMapper.toDto(guardado);
    }

    // HU-06: Listado paginado y filtros
    @Transactional(readOnly = true)
    public Page<ProductoDTO> listarProductos(
        int page,
        String nombre,
        String estado) {

        Long empresaId = obtenerEmpresaId();

        String filtroNombre =
            nombre == null || nombre.isBlank()
                ? null
                : nombre.trim();

        Boolean filtroEstado = null;

        if (estado != null && !estado.isBlank()) {
            switch (estado.toUpperCase(Locale.ROOT)) {
                case "ACTIVO" -> filtroEstado = true;
                case "INACTIVO" -> filtroEstado = false;
                default -> throw new IllegalArgumentException(
                    "El estado debe ser ACTIVO o INACTIVO."
                );
            }
        }

        PageRequest pageable = PageRequest.of(
            page,
            10,
            Sort.by(
                Sort.Order.desc("creadoEn"),
                Sort.Order.desc("id")
            )
        );

        return productoRepository.buscarProductos(
            empresaId,
            representanteFiltro(),
            filtroNombre,
            filtroEstado,
            pageable
        ).map(productoMapper::toDto);
    }

    // HU-06: Detalle del producto
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {

        Long empresaId = obtenerEmpresaId();
        Long representanteId = representanteFiltro();

        // Un representante solo ve los productos que tiene asignados.
        Producto producto = (representanteId == null
            ? productoRepository.findByIdAndEmpresaId(id, empresaId)
            : productoRepository.findByIdAndEmpresaIdAndRepresentanteId(
            id, empresaId, representanteId))
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        return productoMapper.toDto(producto);
    }

    // Conservado para compatibilidad
    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerTodosPorEmpresa() {
        Long representanteId = representanteFiltro();

        List<Producto> productos = productoRepository.findByEmpresaId(
            obtenerEmpresaId()
        );

        return productoMapper.toDtoList(
            representanteId == null
                ? productos
                : productos.stream()
                .filter(p -> representanteId.equals(p.getRepresentanteId()))
                .toList()
        );
    }

    // Conservado para compatibilidad
    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerActivosPorEmpresa() {
        Long representanteId = representanteFiltro();

        List<Producto> productos = productoRepository.findByEmpresaIdAndEsActivoTrue(
            obtenerEmpresaId()
        );

        return productoMapper.toDtoList(
            representanteId == null
                ? productos
                : productos.stream()
                .filter(p -> representanteId.equals(p.getRepresentanteId()))
                .toList()
        );
    }

    @Transactional
    public ProductoDTO actualizarProducto(
        Long id,
        ProductoRequestDTO request) {

        exigirNoRepresentante();

        Long empresaId = obtenerEmpresaId();

        Producto producto = productoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        String nombre = normalizarNombre(request.nombre());

        validarNombreUnico(empresaId, nombre, id);

        producto.setNombre(nombre);
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(request.categoria());

        if (request.esActivo() != null) {
            producto.setEsActivo(request.esActivo());
        }

        // Si la petición no trae representanteId se conserva el actual
        // (p. ej. el botón "Activar" del frontend reenvía solo los demás campos).
        if (request.representanteId() != null) {
            validarRepresentante(empresaId, request.representanteId());
            producto.setRepresentanteId(request.representanteId());
        }

        producto.setActualizadoEn(OffsetDateTime.now());

        return productoMapper.toDto(
            productoRepository.save(producto)
        );
    }

    @Transactional
    public void desactivarProducto(Long id) {

        exigirNoRepresentante();

        Long empresaId = obtenerEmpresaId();

        Producto producto = productoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        producto.setEsActivo(false);
        producto.setActualizadoEn(OffsetDateTime.now());

        productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {

        exigirNoRepresentante();

        Long empresaId = obtenerEmpresaId();

        Producto producto = productoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        productoRepository.delete(producto);
    }
}

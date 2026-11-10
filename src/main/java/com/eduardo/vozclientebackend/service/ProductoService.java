
package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.exceptions.ProductoDuplicadoException;
import com.eduardo.vozclientebackend.mappers.ProductoMapper;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final UsuarioActualService usuarioActualService;

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

        Long empresaId = obtenerEmpresaId();
        String nombre = normalizarNombre(request.nombre());

        validarNombreUnico(empresaId, nombre, null);

        Producto producto = productoMapper.toEntity(request);

        producto.setEmpresaId(empresaId);
        producto.setNombre(nombre);
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(request.categoria());

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
            filtroNombre,
            filtroEstado,
            pageable
        ).map(productoMapper::toDto);
    }

    // HU-06: Detalle del producto
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {

        Long empresaId = obtenerEmpresaId();

        Producto producto = productoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        return productoMapper.toDto(producto);
    }

    // Conservado para compatibilidad
    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerTodosPorEmpresa() {
        return productoMapper.toDtoList(
            productoRepository.findByEmpresaId(
                obtenerEmpresaId()
            )
        );
    }

    // Conservado para compatibilidad
    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerActivosPorEmpresa() {
        return productoMapper.toDtoList(
            productoRepository.findByEmpresaIdAndEsActivoTrue(
                obtenerEmpresaId()
            )
        );
    }

    @Transactional
    public ProductoDTO actualizarProducto(
        Long id,
        ProductoRequestDTO request) {

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

        producto.setActualizadoEn(OffsetDateTime.now());

        return productoMapper.toDto(
            productoRepository.save(producto)
        );
    }

    @Transactional
    public void desactivarProducto(Long id) {

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

        Long empresaId = obtenerEmpresaId();

        Producto producto = productoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto solicitado."
            ));

        productoRepository.delete(producto);
    }
}

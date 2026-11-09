package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.mappers.ProductoMapper;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final UsuarioActualService usuarioActualService;

    @Transactional
    public ProductoDTO crearProducto(ProductoRequestDTO request) {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        if (empresaId == null) {
            throw new RuntimeException("El usuario actual no tiene una empresa asignada.");
        }

        // Mapear desde el nuevo RequestDTO
        Producto producto = productoMapper.toEntity(request);

        // Forzamos el empresaId del usuario autenticado para evitar suplantaciones
        producto.setEmpresaId(empresaId);

        OffsetDateTime ahora = OffsetDateTime.now();
        producto.setCreadoEn(ahora);
        producto.setActualizadoEn(ahora);

        // Valores por defecto
        if (producto.getEsActivo() == null) {
            producto.setEsActivo(true);
        }
        if (producto.getVersion() == null) {
            producto.setVersion(0);
        }

        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toDto(productoGuardado);
    }

    @Transactional
    public ProductoDTO actualizarProducto(Long id, ProductoRequestDTO request) {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        Producto producto = productoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id + " para esta empresa."));

        // Actualizamos campos permitidos desde el RequestDTO
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setCategoria(request.categoria());

        if (request.esActivo() != null) {
            producto.setEsActivo(request.esActivo());
        }

        producto.setActualizadoEn(OffsetDateTime.now());

        Producto productoActualizado = productoRepository.save(producto);
        return productoMapper.toDto(productoActualizado);
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        Producto producto = productoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id + " para esta empresa."));
        return productoMapper.toDto(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerTodosPorEmpresa() {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        return productoRepository.findByEmpresaId(empresaId)
            .stream()
            .map(productoMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerActivosPorEmpresa() {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        return productoRepository.findByEmpresaIdAndEsActivoTrue(empresaId)
            .stream()
            .map(productoMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void desactivarProducto(Long id) {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        Producto producto = productoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id + " para esta empresa."));

        producto.setEsActivo(false);
        producto.setActualizadoEn(OffsetDateTime.now());
        productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        Long empresaId = usuarioActualService.obtenerCuentaActual().getEmpresaId();

        Producto producto = productoRepository.findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id + " para esta empresa."));

        productoRepository.delete(producto);
    }
}

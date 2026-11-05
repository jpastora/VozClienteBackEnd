package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.mappers.ProductoMapper;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.repository.EmpresaRepository;
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
    private final EmpresaRepository empresaRepository; // <-- Agregamos el repositorio de Empresa

    @Transactional
    public ProductoDTO crearProducto(ProductoDTO productoDTO) {

        // 1. Validar que vengan datos y la empresa exista en la BD
        if (productoDTO.empresaId() == null) {
            throw new RuntimeException("El empresaId es obligatorio para crear un producto.");
        }
        if (!empresaRepository.existsById(productoDTO.empresaId())) {
            throw new RuntimeException("No se encontró la empresa con ID: " + productoDTO.empresaId());
        }

        // 2. Mapear
        Producto producto = productoMapper.toEntity(productoDTO);

        OffsetDateTime ahora = OffsetDateTime.now();
        producto.setCreadoEn(ahora);
        producto.setActualizadoEn(ahora);

        // 3. Valores por defecto
        if (producto.getEsActivo() == null) {
            producto.setEsActivo(true);
        }
        if (producto.getVersion() == null) {
            producto.setVersion(0);
        }

        // 4. Guardar
        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toDto(productoGuardado);
    }


    @Transactional
    public ProductoDTO actualizarProducto(Long id, ProductoDTO productoDTO) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id));

        // Actualizamos campos permitidos
        producto.setNombre(productoDTO.nombre());
        producto.setDescripcion(productoDTO.descripcion());
        producto.setCategoria(productoDTO.categoria());

        if (productoDTO.esActivo() != null) {
            producto.setEsActivo(productoDTO.esActivo());
        }

        producto.setActualizadoEn(OffsetDateTime.now());

        Producto productoActualizado = productoRepository.save(producto);
        return productoMapper.toDto(productoActualizado);
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id));
        return productoMapper.toDto(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerTodosPorEmpresa(Long empresaId) {
        return productoRepository.findByEmpresaId(empresaId)
            .stream()
            .map(productoMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerActivosPorEmpresa(Long empresaId) {
        return productoRepository.findByEmpresaIdAndEsActivoTrue(empresaId)
            .stream()
            .map(productoMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void desactivarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("No se encontró el producto con ID: " + id));

        producto.setEsActivo(false);
        producto.setActualizadoEn(OffsetDateTime.now());
        productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("No se encontró el producto con ID: " + id);
        }
        productoRepository.deleteById(id);
    }
}

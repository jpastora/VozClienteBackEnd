package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.dto.ProductoRequestDTO;
import com.eduardo.vozclientebackend.models.Producto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    // Convierte de Entidad a DTO de salida (usado para enviar datos al cliente)
    ProductoDTO toDto(Producto producto);

    // Convierte del DTO de entrada a Entidad (usado al crear el producto)
    Producto toEntity(ProductoRequestDTO request);

    // Convierte una lista de Entidades a una lista de DTOs
    List<ProductoDTO> toDtoList(List<Producto> productos);

    // (Opcional) Si en algún momento necesitas mapear una lista de requests a entidades
    List<Producto> toEntityList(List<ProductoRequestDTO> requests);
}

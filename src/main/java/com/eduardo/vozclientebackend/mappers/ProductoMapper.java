package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.ProductoDTO;
import com.eduardo.vozclientebackend.models.Producto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    ProductoDTO toDto(Producto producto);

    Producto toEntity(ProductoDTO productoDTO);
}

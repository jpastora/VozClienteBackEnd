package com.eduardo.vozclientebackend.mappers;



import com.eduardo.vozclientebackend.dto.CompraDTO;
import com.eduardo.vozclientebackend.models.Compra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompraMapper {

    // De Entidad a DTO (extraemos los IDs de los objetos)
    @Mapping(source = "empresa.id", target = "empresaId")
    @Mapping(source = "paquete.id", target = "paqueteId")
    @Mapping(source = "cuenta.id", target = "cuentaId")
    CompraDTO toDto(Compra compra);

    // De DTO a Entidad (MapStruct creará un objeto vacío con ese ID)
    @Mapping(source = "empresaId", target = "empresa.id")
    @Mapping(source = "paqueteId", target = "paquete.id")
    @Mapping(source = "cuentaId", target = "cuenta.id")
    Compra toEntity(CompraDTO compraDTO);

    List<CompraDTO> toDtoList(List<Compra> compras);

    List<Compra> toEntityList(List<CompraDTO> compraDTOs);
}










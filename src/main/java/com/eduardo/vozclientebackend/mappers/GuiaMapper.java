package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.GuiaDTO;
import com.eduardo.vozclientebackend.models.Guia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GuiaMapper {

    @Mapping(source = "empresa.id", target = "empresaId")
    @Mapping(source = "cuenta.id", target = "cuentaId")
    GuiaDTO toDTO(Guia guia);

    List<GuiaDTO> toDTOs(List<Guia> guias);

    // Si necesitas el mapeo inverso (de DTO a Entidad)
    @Mapping(source = "empresaId", target = "empresa.id")
    @Mapping(source = "cuentaId", target = "cuenta.id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Guia toEntity(GuiaDTO guiaDTO);
}

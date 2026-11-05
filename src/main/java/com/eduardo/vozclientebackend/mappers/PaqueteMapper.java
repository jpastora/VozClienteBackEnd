package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.PaqueteDTO;
import com.eduardo.vozclientebackend.models.Paquete;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaqueteMapper {

    // Convierte de Entidad a DTO
    PaqueteDTO toDto(Paquete paquete);

    // Convierte de DTO a Entidad
    Paquete toEntity(PaqueteDTO paqueteDTO);

    // Convierte una lista de Entidades a una lista de DTOs
    List<PaqueteDTO> toDtoList(List<Paquete> paquetes);

    // Convierte una lista de DTOs a una lista de Entidades
    List<Paquete> toEntityList(List<PaqueteDTO> paqueteDTOs);
}

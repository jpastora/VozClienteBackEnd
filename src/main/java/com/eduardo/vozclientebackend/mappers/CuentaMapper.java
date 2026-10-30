package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.CuentaDTO;
import com.eduardo.vozclientebackend.models.Cuenta;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CuentaMapper {

    // Convierte de Entidad a DTO
    CuentaDTO toDto(Cuenta cuenta);

    // Convierte de DTO a Entidad
    Cuenta toEntity(CuentaDTO cuentaDTO);

    // Convierte una lista de Entidades a una lista de DTOs
    List<CuentaDTO> toDtoList(List<Cuenta> cuentas);

    // Convierte una lista de DTOs a una lista de Entidades
    List<Cuenta> toEntityList(List<CuentaDTO> cuentaDTOs);
}

package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.EmpresaDTO;
import com.eduardo.vozclientebackend.models.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmpresaMapper {

    EmpresaDTO toDto(Empresa empresa);

    List<EmpresaDTO> toDtoList(List<Empresa> empresas);

    @Mapping(target = "id", ignore = true)
    Empresa toEntity(EmpresaDTO empresaDTO);
}

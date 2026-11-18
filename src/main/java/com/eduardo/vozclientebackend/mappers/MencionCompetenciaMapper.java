package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.MencionCompetenciaDTO;
import com.eduardo.vozclientebackend.models.MencionCompetencia;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MencionCompetenciaMapper {
    MencionCompetenciaDTO toDto(MencionCompetencia entity);
    MencionCompetencia toEntity(MencionCompetenciaDTO dto);
    List<MencionCompetenciaDTO> toDtoList(List<MencionCompetencia> entities);
}

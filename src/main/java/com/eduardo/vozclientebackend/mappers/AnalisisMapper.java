package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.AnalisisDTO;
import com.eduardo.vozclientebackend.models.Analisis;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {MencionCompetenciaMapper.class})
public interface AnalisisMapper {

    AnalisisDTO toDto(Analisis entity);

    Analisis toEntity(AnalisisDTO dto);

    List<AnalisisDTO> toDtoList(List<Analisis> entities);
}

package com.eduardo.vozclientebackend.mappers;


import com.eduardo.vozclientebackend.dto.SalaDTO;
import com.eduardo.vozclientebackend.models.Sala;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SalaMapper {
    SalaDTO toDto(Sala sala);
    List<SalaDTO> toDtoList(List<Sala> salas);

    @Mapping(target = "id", ignore = true)
    Sala toEntity(SalaDTO salaDTO);
}

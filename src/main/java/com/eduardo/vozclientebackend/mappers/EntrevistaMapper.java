package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.EntrevistaDTO;
import com.eduardo.vozclientebackend.models.Entrevista;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntrevistaMapper {

    EntrevistaDTO toDto(Entrevista entrevista);

    List<EntrevistaDTO> toDtoList(List<Entrevista> entrevistas);

    @Mapping(target = "id", ignore = true)
    Entrevista toEntity(EntrevistaDTO entrevistaDTO);
}

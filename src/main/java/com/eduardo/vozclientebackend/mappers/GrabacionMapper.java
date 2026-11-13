package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.GrabacionDTO;
import com.eduardo.vozclientebackend.models.Grabacion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GrabacionMapper {

    GrabacionDTO toDto(Grabacion grabacion);

    Grabacion toEntity(GrabacionDTO grabacionDTO);
}

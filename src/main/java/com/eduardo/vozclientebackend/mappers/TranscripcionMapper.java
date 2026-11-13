package com.eduardo.vozclientebackend.mappers;

import com.eduardo.vozclientebackend.dto.TranscripcionDTO;
import com.eduardo.vozclientebackend.models.Transcripcion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TranscripcionMapper {

    TranscripcionDTO toDto(Transcripcion entity);

    Transcripcion toEntity(TranscripcionDTO dto);
}

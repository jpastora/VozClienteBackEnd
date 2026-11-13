package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.FragmentoTranscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FragmentoTranscripcionRepository
    extends JpaRepository<FragmentoTranscripcion, Long> {

    List<FragmentoTranscripcion>
    findByTranscripcionIdOrderBySegundoInicioAsc(
        Long transcripcionId);
}

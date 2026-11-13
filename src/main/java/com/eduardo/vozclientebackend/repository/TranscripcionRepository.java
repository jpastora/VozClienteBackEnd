package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import com.eduardo.vozclientebackend.models.Transcripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscripcionRepository extends JpaRepository<Transcripcion, Long> {

    Optional<Transcripcion> findByGrabacionId(Long grabacionId);

    boolean existsByGrabacionId(Long grabacionId);

    List<Transcripcion> findByEstado(EstadoTranscripcion estado);

    List<Transcripcion> findByGrabacionIdIn(List<Long> grabacionIds);

}

package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Analisis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalisisRepository
    extends JpaRepository<Analisis, Long> {

    List<Analisis> findByTranscripcionIdIn(
        List<Long> transcripcionIds);


    Optional<Analisis> findByTranscripcionId(Long transcripcionId);

}

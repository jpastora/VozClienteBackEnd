package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.MencionCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MencionCompetenciaRepository
    extends JpaRepository<MencionCompetencia, Long> {

    List<MencionCompetencia>
    findByAnalisisIdOrderByFrecuenciaDesc(
        Long analisisId);
}

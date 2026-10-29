package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.models.Entrevista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntrevistaRepository extends JpaRepository<Entrevista, Long> {

    // Obtener todas las entrevistas vinculadas a una sala específica
    List<Entrevista> findBySalaId(Long salaId);

    // Obtener todas las entrevistas que ha realizado un representante/agente
    List<Entrevista> findByRepresentanteId(Long representanteId);

    // Filtrar entrevistas por su estado (EN_CURSO, FINALIZADA, FALLIDA)
    List<Entrevista> findByEstado(EstadoEntrevista estado);
}

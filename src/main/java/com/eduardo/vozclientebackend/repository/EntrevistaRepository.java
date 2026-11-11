package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.models.Entrevista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface EntrevistaRepository
    extends JpaRepository<Entrevista, Long> {

    List<Entrevista> findBySalaId(Long salaId);

    List<Entrevista> findByRepresentanteId(
        Long representanteId);

    List<Entrevista> findByEstado(
        EstadoEntrevista estado);

    List<Entrevista> findByEstadoAndInicioEnBefore(
        EstadoEntrevista estado,
        OffsetDateTime limite);

    @Query("""
        SELECT e
        FROM Entrevista e, Sala s, Producto p
        WHERE e.salaId = s.id
          AND s.productoId = p.id
          AND p.empresaId = :empresaId
          AND (:productoId IS NULL
               OR p.id = :productoId)
          AND (:estado IS NULL
               OR e.estado = :estado)
        """)
    Page<Entrevista> buscarEntrevistas(
        @Param("empresaId") Long empresaId,
        @Param("productoId") Long productoId,
        @Param("estado") EstadoEntrevista estado,
        Pageable pageable);
}

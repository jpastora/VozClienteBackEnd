package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoTranscripcion;
import com.eduardo.vozclientebackend.models.Transcripcion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscripcionRepository
    extends JpaRepository<Transcripcion, Long> {

    Optional<Transcripcion> findByGrabacionId(Long grabacionId);

    boolean existsByGrabacionId(Long grabacionId);

    List<Transcripcion> findByEstado(
        EstadoTranscripcion estado
    );

    List<Transcripcion> findByGrabacionIdIn(
        List<Long> grabacionIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM Transcripcion t
        WHERE t.id = :id
        """)
    Optional<Transcripcion> findByIdParaActualizar(
        @Param("id") Long id
    );

    @Query("""
        SELECT t.id
        FROM Transcripcion t
        WHERE t.estado = :estado
          AND t.texto IS NOT NULL
          AND NOT EXISTS (
              SELECT a.id
              FROM Analisis a
              WHERE a.transcripcionId = t.id
          )
        ORDER BY t.id
        """)
    List<Long> buscarIdsSinAnalisis(
        @Param("estado") EstadoTranscripcion estado
    );
}

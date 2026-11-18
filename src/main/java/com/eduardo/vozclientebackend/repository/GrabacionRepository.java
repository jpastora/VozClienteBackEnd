package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.models.Grabacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrabacionRepository extends JpaRepository<Grabacion, Long> {


    Optional<Grabacion> findByEntrevistaId(Long entrevistaId);

    /**
     * Igual que findByEntrevistaId pero con bloqueo de escritura: serializa los fragmentos
     * de una misma entrevista para que no se anexen fuera de orden. Solo dentro de una transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Grabacion g where g.entrevistaId = :entrevistaId")
    Optional<Grabacion> findByEntrevistaIdParaActualizar(@Param("entrevistaId") Long entrevistaId);

    boolean existsByEntrevistaId(Long entrevistaId);

    void deleteByEntrevistaId(Long entrevistaId);

    List<Grabacion> findByEstado(EstadoGrabacion estado);

    /** Grabaciones en curso cuyo último fragmento llegó antes del límite (abandonadas). */
    List<Grabacion> findByEstadoAndUltimoFragmentoEnBefore(EstadoGrabacion estado, OffsetDateTime limite);

    List<Grabacion> findByEntrevistaIdIn(List<Long> entrevistaIds);

}

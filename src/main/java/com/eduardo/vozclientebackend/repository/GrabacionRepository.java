package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.models.Grabacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrabacionRepository extends JpaRepository<Grabacion, Long> {


    Optional<Grabacion> findByEntrevistaId(Long entrevistaId);

    boolean existsByEntrevistaId(Long entrevistaId);

    void deleteByEntrevistaId(Long entrevistaId);

    List<Grabacion> findByEstado(EstadoGrabacion estado);


    List<Grabacion> findByEntrevistaIdIn(List<Long> entrevistaIds);

}

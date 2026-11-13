package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaqueteRepository extends JpaRepository<Paquete, Long> {


    Optional<Paquete> findByNombre(String nombre);


    List<Paquete> findByEsActivoTrue();


    List<Paquete> findByEsActivoFalse();
}

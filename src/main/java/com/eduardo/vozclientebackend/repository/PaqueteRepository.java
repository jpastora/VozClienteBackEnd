package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaqueteRepository extends JpaRepository<Paquete, Long> {


    Optional<Paquete> findByNombre(String nombre);

    /** El nombre del paquete es único sin importar mayúsculas (RF-17). */
    Optional<Paquete> findByNombreIgnoreCase(String nombre);

    /** Quita la marca de recomendado a todos los paquetes salvo al indicado (como máximo hay uno). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Paquete p SET p.esRecomendado = false WHERE p.esRecomendado = true AND p.id <> :id")
    int quitarRecomendadoExcepto(@Param("id") Long id);


    List<Paquete> findByEsActivoTrue();


    List<Paquete> findByEsActivoFalse();
}

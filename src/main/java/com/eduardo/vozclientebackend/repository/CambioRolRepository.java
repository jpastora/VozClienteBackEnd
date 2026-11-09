package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.CambioRol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CambioRolRepository
    extends JpaRepository<CambioRol, Long> {

    // Obtener el historial de cambios de una cuenta
    List<CambioRol> findByCuentaIdOrderByCreadoEnDesc(
        Long cuentaId
    );
}

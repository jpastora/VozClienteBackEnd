package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Guia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuiaRepository extends JpaRepository<Guia, Long> {

    // Métodos útiles para filtrar guías según la empresa o el creador (cuenta)
    List<Guia> findAllByEmpresaId(Long empresaId);

    List<Guia> findAllByCuentaId(Long cuentaId);
}

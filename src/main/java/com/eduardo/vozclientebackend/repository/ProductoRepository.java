package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Producto> findByEmpresaId(Long empresaId);

    List<Producto> findByEmpresaIdAndEsActivoTrue(Long empresaId);
}

package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByEmpresaId(Long empresaId);

    List<Producto> findByEmpresaIdAndEsActivoTrue(Long empresaId);

    List<Producto> findByEsActivoTrue();

}

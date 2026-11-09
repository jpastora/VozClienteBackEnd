package com.eduardo.vozclientebackend.repository;


import com.eduardo.vozclientebackend.models.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByEmpresa(Long EmpresaId);


    List<Compra> findByCuentaId(Long cuentaId);


    List<Compra> findByPaquete(Long paqueteId);

}

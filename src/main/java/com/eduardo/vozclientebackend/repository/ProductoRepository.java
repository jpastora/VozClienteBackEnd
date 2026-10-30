package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;



public interface ProductoRepository extends JpaRepository<Producto, Long> {



}

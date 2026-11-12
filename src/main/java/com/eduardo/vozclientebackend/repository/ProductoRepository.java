
package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository
    extends JpaRepository<Producto, Long> {

    Optional<Producto> findByIdAndEmpresaId(
        Long id,
        Long empresaId
    );

    List<Producto> findByEmpresaId(
        Long empresaId
    );

    List<Producto> findByEmpresaIdAndEsActivoTrue(
        Long empresaId
    );

    // Listado paginado con filtros opcionales
    // por nombre y estado.
    @Query("""
    SELECT p
    FROM Producto p
    WHERE p.empresaId = :empresaId
      AND (CAST(:representanteId AS Long) IS NULL
           OR p.representanteId = :representanteId)
      AND (
          :nombre IS NULL
          OR LOWER(p.nombre) LIKE
             CONCAT('%', LOWER(CAST(:nombre AS String)), '%')
      )
      AND (
          :estado IS NULL
          OR p.esActivo = :estado
      )
    """)
    Page<Producto> buscarProductos(
        @Param("empresaId") Long empresaId,
        @Param("representanteId") Long representanteId,
        @Param("nombre") String nombre,
        @Param("estado") Boolean estado,
        Pageable pageable
    );

    Optional<Producto> findByIdAndEmpresaIdAndRepresentanteId(
        Long id, Long empresaId, Long representanteId);

    // Validar nombres duplicados dentro de la empresa.
    // Permite excluir el producto actual al editar.
    @Query("""
        SELECT COUNT(p) > 0
        FROM Producto p
        WHERE p.empresaId = :empresaId
          AND LOWER(p.nombre) = LOWER(:nombre)
          AND (
              :excluirId IS NULL
              OR p.id <> :excluirId
          )
        """)
    boolean existeNombreEnEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("nombre") String nombre,
        @Param("excluirId") Long excluirId
    );
}

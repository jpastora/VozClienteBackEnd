package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoPago;
import com.eduardo.vozclientebackend.models.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** Historial de compras de una empresa, la más reciente primero. */
    List<Compra> findByEmpresaIdOrderByCreadoEnDesc(Long empresaId);

    List<Compra> findByCuentaId(Long cuentaId);

    long countByCuentaId(Long cuentaId);

    long countByEmpresaId(Long empresaId);

    List<Compra> findByPaqueteId(Long paqueteId);

    /** Si existe, la compra ya registrada con esa clave (doble envío del formulario). */
    Optional<Compra> findByClaveIdempotencia(UUID claveIdempotencia);

    /** Indica si un paquete ya se vendió (entonces se desactiva, no se elimina). */
    boolean existsByPaqueteId(Long paqueteId);

    /** Suma de las salas acreditadas en las compras de la empresa con el estado de pago dado. */
    @Query("""
        SELECT COALESCE(SUM(c.salasAcreditadas), 0)
        FROM Compra c
        WHERE c.empresa.id = :empresaId
          AND c.estadoPago = :estado
        """)
    Long sumarSalasPorEstado(@Param("empresaId") Long empresaId, @Param("estado") EstadoPago estado);
}

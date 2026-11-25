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

    /** Cambios de rol que esta cuenta hizo sobre otras cuentas (auditoría que no se debe perder). */
    long countByAutorIdAndCuentaIdNot(Long autorId, Long cuentaId);

    /** Historial propio de una cuenta, que se va con ella al eliminarla. */
    void deleteByCuentaId(Long cuentaId);
}

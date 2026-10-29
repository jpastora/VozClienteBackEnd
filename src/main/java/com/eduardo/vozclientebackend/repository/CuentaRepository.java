package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    // Búsqueda por correo (útil para autenticación, ya que es unique)
    Optional<Cuenta> findByCorreo(String correo);

    // Búsqueda por Google Subject (útil para OAuth2/Google Login)
    Optional<Cuenta> findByGoogleSub(String googleSub);

    // Obtener todas las cuentas asociadas a una empresa específica
    List<Cuenta> findByEmpresaId(Long empresaId);

    // Obtener cuentas por rol
    List<Cuenta> findByRol(RolCuenta rol);

    // Obtener cuentas por estado (ej. ACTIVAS, INACTIVAS)
    List<Cuenta> findByEstado(EstadoCuenta estado);

    // Obtener cuentas de una empresa por estado
    List<Cuenta> findByEmpresaIdAndEstado(Long empresaId, EstadoCuenta estado);
}

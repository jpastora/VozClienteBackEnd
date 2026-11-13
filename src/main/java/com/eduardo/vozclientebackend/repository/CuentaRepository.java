package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.enums.RolCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    // Buscar una cuenta por correo electrónico
    Optional<Cuenta> findByCorreo(String correo);

    // Buscar por identificador de Google
    Optional<Cuenta> findByGoogleSub(String googleSub);

    // Listar las cuentas de una empresa
    List<Cuenta> findByEmpresaId(Long empresaId);

    // Buscar cuentas por rol
    List<Cuenta> findByRol(RolCuenta rol);

    // Buscar cuentas por estado
    List<Cuenta> findByEstado(EstadoCuenta estado);

    // Buscar cuentas de una empresa por estado
    List<Cuenta> findByEmpresaIdAndEstado(
        Long empresaId,
        EstadoCuenta estado
    );

    // Buscar una cuenta específica dentro de una empresa
    Optional<Cuenta> findByIdAndEmpresaId(
        Long id,
        Long empresaId
    );

    // Listar representantes de una empresa
    List<Cuenta> findByEmpresaIdAndRolOrderByNombreCompletoAsc(
        Long empresaId,
        RolCuenta rol
    );

    // Buscar un representante específico
    Optional<Cuenta> findByIdAndEmpresaIdAndRol(
        Long id,
        Long empresaId,
        RolCuenta rol
    );

    // Comprobar si existe un correo, ignorando mayúsculas
    boolean existsByCorreoIgnoreCase(String correo);

    // Contar administradores activos de una empresa
    long countByEmpresaIdAndRolAndEstado(
        Long empresaId,
        RolCuenta rol,
        EstadoCuenta estado
    );

    // Buscar cuentas por empresa y rol
    List<Cuenta> findByEmpresaIdAndRol(
        Long empresaId,
        RolCuenta rol
    );

    // Contar administradores globales activos
    long countByRolAndEstadoAndEmpresaIdIsNull(
        RolCuenta rol,
        EstadoCuenta estado
    );

    // Buscar una cuenta usando el correo sin distinguir mayúsculas
    @Query("""
            SELECT c
            FROM Cuenta c
            WHERE LOWER(c.correo) = LOWER(:correo)
            """)
    Optional<Cuenta> buscarPorCorreo(
        @Param("correo") String correo
    );
}

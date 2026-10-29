package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.enums.EstadoSala;
import com.eduardo.vozclientebackend.models.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {

    // Busca una sala por su token único
    Optional<Sala> findByToken(String token);

    // Obtiene todas las salas asociadas a un producto específico
    List<Sala> findByProductoId(Long productoId);

    // Obtiene todas las salas creadas por un usuario/cliente específico
    List<Sala> findByCreadoPorId(Long creadoPorId);

    // Obtiene las salas filtradas por su estado (PENDIENTE, RESPONDIDO, EXPIRADO)
    List<Sala> findByEstado(EstadoSala estado);

    // Verifica si existe una sala con un token específico
    boolean existsByToken(String token);
}

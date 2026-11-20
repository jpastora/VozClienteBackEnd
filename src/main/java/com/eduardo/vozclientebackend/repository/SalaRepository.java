package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.dto.ConsumoSalaDTO;
import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.enums.EstadoSala;
import com.eduardo.vozclientebackend.models.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
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

    // ---- Saldo de salas (RF-07, RF-18) ----

    /**
     * Salas de la empresa ya descontadas del saldo: al menos una de sus entrevistas terminó con una
     * grabación correcta. Cada sala se cuenta una sola vez, aunque haya tenido varios intentos.
     */
    @Query("""
        SELECT COUNT(DISTINCT s.id)
        FROM Sala s, Producto p, Entrevista e, Grabacion g
        WHERE s.productoId = p.id
          AND e.salaId = s.id
          AND g.entrevistaId = e.id
          AND p.empresaId = :empresaId
          AND g.estado IN :correctas
        """)
    long contarConsumidasPorEmpresa(@Param("empresaId") Long empresaId,
                                    @Param("correctas") Collection<EstadoGrabacion> correctas);

    /**
     * Enlaces vigentes de la empresa que todavía no se consumen (pendientes, o respondidos cuya
     * grabación aún no termina bien). Reservan saldo para no emitir más enlaces de los comprados.
     */
    @Query("""
        SELECT COUNT(s)
        FROM Sala s, Producto p
        WHERE s.productoId = p.id
          AND p.empresaId = :empresaId
          AND s.estado <> :expirado
          AND s.venceEn > :ahora
          AND NOT EXISTS (
              SELECT 1 FROM Entrevista e, Grabacion g
              WHERE e.salaId = s.id
                AND g.entrevistaId = e.id
                AND g.estado IN :correctas)
        """)
    long contarReservadasPorEmpresa(@Param("empresaId") Long empresaId,
                                    @Param("ahora") OffsetDateTime ahora,
                                    @Param("expirado") EstadoSala expirado,
                                    @Param("correctas") Collection<EstadoGrabacion> correctas);

    /** Historial de consumo: una fila por sala descontada, la más reciente primero. */
    @Query("""
        SELECT new com.eduardo.vozclientebackend.dto.ConsumoSalaDTO(
            s.id, p.id, p.nombre, s.nombreConsumidor, MIN(g.creadoEn))
        FROM Sala s, Producto p, Entrevista e, Grabacion g
        WHERE s.productoId = p.id
          AND e.salaId = s.id
          AND g.entrevistaId = e.id
          AND p.empresaId = :empresaId
          AND g.estado IN :correctas
        GROUP BY s.id, p.id, p.nombre, s.nombreConsumidor
        ORDER BY MIN(g.creadoEn) DESC
        """)
    List<ConsumoSalaDTO> listarConsumosPorEmpresa(@Param("empresaId") Long empresaId,
                                                  @Param("correctas") Collection<EstadoGrabacion> correctas);
}

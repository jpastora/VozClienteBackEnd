package com.eduardo.vozclientebackend.repository;

import com.eduardo.vozclientebackend.models.Analisis;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface EstadisticasRepository
    extends Repository<Analisis, Long> {

    // Proyección para la distribución de intención.

    interface DistribucionIntencionProjection {

        String getIntencion();

        Long getCantidad();
    }

    // Proyección para las menciones de competencia.

    interface CompetidorProjection {

        String getCompetidor();

        Long getMenciones();
    }

    // Relaciones comunes entre las tablas.

    String CONSULTA_BASE = """
        FROM analisis a
        INNER JOIN transcripciones t
            ON t.id = a.transcripcion_id
        INNER JOIN grabaciones g
            ON g.id = t.grabacion_id
        INNER JOIN entrevistas e
            ON e.id = g.entrevista_id
        INNER JOIN salas s
            ON s.id = e.sala_id
        INNER JOIN productos p
            ON p.id = s.producto_id
        """;

    // Filtros comunes para las estadísticas.

    String FILTROS = """
        WHERE p.empresa_id = :empresaId
          AND a.estado = 'COMPLETADA'
          AND (
              CAST(:productoId AS BIGINT) IS NULL
              OR p.id = :productoId
          )
          AND (
              CAST(:desde AS TIMESTAMPTZ) IS NULL
              OR e.inicio_en >= :desde
          )
          AND (
              CAST(:hastaExclusiva AS TIMESTAMPTZ) IS NULL
              OR e.inicio_en < :hastaExclusiva
          )
        """;

    // Obtener la distribución de intención de compra.

    @Query(
        value = """
            SELECT
                a.intencion AS intencion,
                COUNT(DISTINCT e.id) AS cantidad
            """
            + CONSULTA_BASE
            + FILTROS
            + """
            GROUP BY a.intencion
            """,
        nativeQuery = true
    )
    List<DistribucionIntencionProjection> obtenerDistribucion(

        @Param("empresaId")
        Long empresaId,

        @Param("productoId")
        Long productoId,

        @Param("desde")
        OffsetDateTime desde,

        @Param("hastaExclusiva")
        OffsetDateTime hastaExclusiva

    );

    // Obtener los competidores y sus frecuencias acumuladas.

    @Query(
        value = """
            SELECT
                MIN(TRIM(m.competidor)) AS competidor,
                SUM(m.frecuencia) AS menciones
            """
            + CONSULTA_BASE
            + """
            INNER JOIN menciones_competencia m
                ON m.analisis_id = a.id
            """
            + FILTROS
            + """
            GROUP BY LOWER(TRIM(m.competidor))
            ORDER BY menciones DESC, competidor ASC
            """,
        nativeQuery = true
    )
    List<CompetidorProjection> obtenerCompetidores(

        @Param("empresaId")
        Long empresaId,

        @Param("productoId")
        Long productoId,

        @Param("desde")
        OffsetDateTime desde,

        @Param("hastaExclusiva")
        OffsetDateTime hastaExclusiva

    );
}

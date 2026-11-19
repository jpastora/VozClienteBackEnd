package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.PanelGlobalDTO;
import com.eduardo.vozclientebackend.dto.PanelGlobalDTO.ClientePrincipalDTO;
import com.eduardo.vozclientebackend.dto.PanelGlobalDTO.ConteoDTO;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Consultas agregadas de todas las empresas para el panel global (RF-19). */
@Service
@RequiredArgsConstructor
public class PanelGlobalService {

    private static final int MAXIMO_CLIENTES_PRINCIPALES = 5;

    static final String SQL_INGRESOS =
        "SELECT COALESCE(SUM(monto), 0), COUNT(*) FROM compras WHERE estado_pago = 'APROBADO'";

    static final String SQL_EMPRESAS = "SELECT COUNT(*) FROM empresas";

    static final String SQL_ENTREVISTAS_POR_ESTADO =
        "SELECT estado, COUNT(*) FROM entrevistas GROUP BY estado ORDER BY estado";

    static final String SQL_INTENCION =
        "SELECT intencion, COUNT(*) FROM analisis "
            + "WHERE estado = 'COMPLETADA' AND intencion IS NOT NULL GROUP BY intencion ORDER BY intencion";

    static final String SQL_CLIENTES_PRINCIPALES = """
        SELECT e.id, e.nombre,
               COALESCE((SELECT SUM(c.monto) FROM compras c
                          WHERE c.empresa_id = e.id AND c.estado_pago = 'APROBADO'), 0) AS ingresos,
               (SELECT COUNT(*) FROM entrevistas en
                  JOIN salas s ON s.id = en.sala_id
                  JOIN productos p ON p.id = s.producto_id
                 WHERE p.empresa_id = e.id) AS entrevistas
          FROM empresas e
         ORDER BY ingresos DESC, entrevistas DESC, e.nombre ASC
         LIMIT %d
        """.formatted(MAXIMO_CLIENTES_PRINCIPALES);

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PanelGlobalDTO obtener() {
        Object[] ingresos = (Object[]) entityManager.createNativeQuery(SQL_INGRESOS).getSingleResult();
        BigDecimal ingresosTotales = new BigDecimal(ingresos[0].toString());
        long comprasAprobadas = ((Number) ingresos[1]).longValue();

        long totalEmpresas = ((Number) entityManager.createNativeQuery(SQL_EMPRESAS).getSingleResult()).longValue();

        List<ConteoDTO> porEstado = conteos(SQL_ENTREVISTAS_POR_ESTADO);
        long totalEntrevistas = porEstado.stream().mapToLong(ConteoDTO::cantidad).sum();

        List<ConteoDTO> intencion = conteos(SQL_INTENCION);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(SQL_CLIENTES_PRINCIPALES).getResultList();
        List<ClientePrincipalDTO> clientes = filas.stream()
            .map(f -> new ClientePrincipalDTO(
                ((Number) f[0]).longValue(),
                (String) f[1],
                new BigDecimal(f[2].toString()),
                ((Number) f[3]).longValue()))
            .toList();

        return new PanelGlobalDTO(ingresosTotales, comprasAprobadas, totalEmpresas, totalEntrevistas,
            porEstado, intencion, clientes);
    }

    private List<ConteoDTO> conteos(String sql) {
        @SuppressWarnings("unchecked")
        List<Object[]> filas = entityManager.createNativeQuery(sql).getResultList();
        return filas.stream()
            .map(f -> new ConteoDTO((String) f[0], ((Number) f[1]).longValue()))
            .toList();
    }
}

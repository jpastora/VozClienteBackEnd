package com.eduardo.vozclientebackend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Panel global del administrador de la plataforma (RF-19): ingresos, empresas, entrevistas,
 * intención de compra global y clientes principales.
 *
 * @param ingresosTotales    suma de las compras aprobadas de todas las empresas
 * @param comprasAprobadas   cantidad de compras aprobadas
 * @param totalEmpresas      empresas registradas
 * @param totalEntrevistas   entrevistas de todas las empresas
 * @param entrevistasPorEstado cantidad de entrevistas por estado (EN_CURSO, FINALIZADA, FALLIDA)
 * @param intencionGlobal    análisis completados por intención (RECOMPRARIA, NO_RECOMPRARIA, RECHAZA, NEUTRAL)
 * @param clientesPrincipales las empresas que más ingresos generan (hasta 5)
 */
public record PanelGlobalDTO(
    BigDecimal ingresosTotales,
    long comprasAprobadas,
    long totalEmpresas,
    long totalEntrevistas,
    List<ConteoDTO> entrevistasPorEstado,
    List<ConteoDTO> intencionGlobal,
    List<ClientePrincipalDTO> clientesPrincipales
) {

    /** Una categoría y su cantidad. */
    public record ConteoDTO(String clave, long cantidad) {
    }

    /** Empresa con sus ingresos y entrevistas. */
    public record ClientePrincipalDTO(Long empresaId, String nombre, BigDecimal ingresos, long entrevistas) {
    }
}

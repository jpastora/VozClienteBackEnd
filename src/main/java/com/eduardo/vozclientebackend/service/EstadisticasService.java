package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.EstadisticasDTO;
import com.eduardo.vozclientebackend.enums.IntencionAnalisis;
import com.eduardo.vozclientebackend.repository.EstadisticasRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private static final ZoneId ZONA_HORARIA =
        ZoneId.of("America/Costa_Rica");

    private final EstadisticasRepository estadisticasRepository;

    private final ProductoRepository productoRepository;

    private final UsuarioActualService usuarioActualService;

    @Transactional(
        readOnly = true,
        isolation = Isolation.REPEATABLE_READ
    )
    public EstadisticasDTO obtenerEstadisticas(

        Long productoId,

        LocalDate desde,

        LocalDate hasta

    ) {

        // Obtener la empresa del usuario autenticado.

        Long empresaId =
            usuarioActualService.empresaEfectiva();

        // Validar los filtros recibidos.

        validarFechas(desde, hasta);

        validarProducto(productoId, empresaId);

        // Convertir las fechas a la zona horaria local.

        OffsetDateTime fechaDesde = desde == null

            ? null

            : desde.atStartOfDay(ZONA_HORARIA)
            .toOffsetDateTime();

        OffsetDateTime fechaHastaExclusiva = hasta == null

            ? null

            : hasta.plusDays(1)
            .atStartOfDay(ZONA_HORARIA)
            .toOffsetDateTime();

        // Consultar la distribución de intención.

        List<EstadisticasRepository.DistribucionIntencionProjection>
            resultados = estadisticasRepository.obtenerDistribucion(

            empresaId,

            productoId,

            fechaDesde,

            fechaHastaExclusiva

        );

        Map<IntencionAnalisis, Long> cantidades =
            new EnumMap<>(IntencionAnalisis.class);

        long totalEntrevistas = 0;

        // Procesar las proyecciones del repositorio.

        for (var resultado : resultados) {

            long cantidad = resultado.getCantidad();

            totalEntrevistas += cantidad;

            String valorIntencion =
                resultado.getIntencion();

            if (valorIntencion != null) {

                IntencionAnalisis intencion =
                    IntencionAnalisis.valueOf(valorIntencion);

                cantidades.merge(

                    intencion,

                    cantidad,

                    Long::sum

                );
            }
        }

        // Construir la distribución completa.

        List<EstadisticasDTO.DistribucionIntencionDTO>
            distribucion = new ArrayList<>();

        for (IntencionAnalisis intencion
            : IntencionAnalisis.values()) {

            long cantidad =
                cantidades.getOrDefault(intencion, 0L);

            distribucion.add(

                new EstadisticasDTO.DistribucionIntencionDTO(

                    intencion,

                    cantidad,

                    calcularPorcentaje(
                        cantidad,
                        totalEntrevistas
                    )

                )
            );
        }

        // Calcular el porcentaje de intención de compra.

        long recomprarian = cantidades.getOrDefault(

            IntencionAnalisis.RECOMPRARIA,

            0L

        );

        double intencionCompra = calcularPorcentaje(

            recomprarian,

            totalEntrevistas

        );

        // Consultar las menciones de competencia.

        List<EstadisticasDTO.CompetidorDTO> competidores =
            estadisticasRepository.obtenerCompetidores(

                    empresaId,

                    productoId,

                    fechaDesde,

                    fechaHastaExclusiva

                )
                .stream()
                .map(resultado ->

                    new EstadisticasDTO.CompetidorDTO(

                        resultado.getCompetidor(),

                        resultado.getMenciones()

                    )

                )
                .toList();

        // Calcular la cantidad total de menciones.

        long totalMenciones = competidores.stream()

            .mapToLong(
                EstadisticasDTO.CompetidorDTO::menciones
            )

            .sum();

        // Retornar el resultado final.

        return new EstadisticasDTO(

            totalEntrevistas,

            intencionCompra,

            totalMenciones,

            distribucion,

            competidores

        );
    }

    private void validarFechas(

        LocalDate desde,

        LocalDate hasta

    ) {

        if (desde != null
            && hasta != null
            && desde.isAfter(hasta)) {

            throw new ResponseStatusException(

                HttpStatus.BAD_REQUEST,

                "La fecha desde no puede ser posterior "
                    + "a la fecha hasta."

            );
        }
    }

    private void validarProducto(

        Long productoId,

        Long empresaId

    ) {

        if (productoId == null) {
            return;
        }

        productoRepository.findByIdAndEmpresaId(

            productoId,

            empresaId

        ).orElseThrow(() ->

            new ResponseStatusException(

                HttpStatus.NOT_FOUND,

                "El producto no existe o no está disponible."

            )
        );
    }

    private double calcularPorcentaje(

        long cantidad,

        long total

    ) {

        if (total == 0) {
            return 0.0;
        }

        double porcentaje =
            cantidad * 100.0 / total;

        return Math.round(porcentaje * 100.0) / 100.0;
    }
}

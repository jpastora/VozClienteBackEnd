package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.SalaDTO;
import com.eduardo.vozclientebackend.enums.EstadoSala;
import com.eduardo.vozclientebackend.mappers.SalaMapper;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.models.Producto;
import com.eduardo.vozclientebackend.models.Sala;
import com.eduardo.vozclientebackend.repository.CuentaRepository;
import com.eduardo.vozclientebackend.repository.ProductoRepository;
import com.eduardo.vozclientebackend.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;
    private final CuentaRepository cuentaRepository;
    private final ProductoRepository productoRepository;
    private final SalaMapper salaMapper;

    /**
     * Crea una nueva sala con los datos básicos.
     * Valida que la cuenta y el producto existan antes de guardar.
     */
    @Transactional
    public SalaDTO crearSala(SalaDTO salaDTO) {
        if (salaDTO == null) {
            throw new IllegalArgumentException("Los datos de la sala son obligatorios");
        }
        if (salaDTO.creadoPorId() == null) {
            throw new IllegalArgumentException("La cuenta creadora es obligatoria");
        }
        if (salaDTO.productoId() == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }

        // 1. CAPTURAR las entidades reales de la BD
        Cuenta cuenta = cuentaRepository.findById(salaDTO.creadoPorId())
            .orElseThrow(() -> new RuntimeException(
                "La cuenta especificada no existe con ID: " + salaDTO.creadoPorId()));

        Producto producto = productoRepository.findById(salaDTO.productoId())
            .orElseThrow(() -> new RuntimeException(
                "El producto especificado no existe con ID: " + salaDTO.productoId()));

        // 2. Mapear los datos básicos
        Sala sala = salaMapper.toEntity(salaDTO);

        // 3. ASIGNAR las entidades a la sala (¡Esta es la clave para que no guarden null!)
        sala.setProductoId(salaDTO.productoId());
        sala.setCreadoPorId(salaDTO.creadoPorId());
        // 4. Generar token y fechas
        sala.setToken(generarTokenUnico());
        sala.setEstado(EstadoSala.PENDIENTE);
        OffsetDateTime ahora = OffsetDateTime.now();
        sala.setEmitidaEn(ahora);

        if (sala.getVenceEn() == null) {
            sala.setVenceEn(ahora.plusDays(7));
        }

        Sala salaGuardada = salaRepository.save(sala);
        return salaMapper.toDto(salaGuardada);
    }

    /**
     * Obtiene una sala por su token único.
     */
    @Transactional(readOnly = true)
    public SalaDTO obtenerPorToken(String token) {
        Sala sala = salaRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Sala no encontrada con el token proporcionado"));
        return salaMapper.toDto(sala);
    }

    /**
     * Lista todas las salas de un producto específico.
     */
    @Transactional(readOnly = true)
    public List<SalaDTO> obtenerPorProducto(Long productoId) {
        // Opcional: Validar que el producto exista antes de buscar sus salas
        productoRepository.findById(productoId)
            .orElseThrow(() -> new RuntimeException("El producto no existe con ID: " + productoId));

        List<Sala> salas = salaRepository.findByProductoId(productoId);
        return salaMapper.toDtoList(salas);
    }

    /**
     * Cambia el estado de la sala a RESPONDIDO y registra cuándo aceptó la grabación el consumidor.
     */
    @Transactional
    public SalaDTO responderSala(String token, String nombreConsumidor) {
        Sala sala = salaRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Sala no encontrada"));

        if (sala.getEstado() == EstadoSala.EXPIRADO || sala.getVenceEn().isBefore(OffsetDateTime.now())) {
            sala.setEstado(EstadoSala.EXPIRADO);
            salaRepository.save(sala);
            throw new RuntimeException("La sala ya ha expirado");
        }

        sala.setEstado(EstadoSala.RESPONDIDO);
        sala.setNombreConsumidor(nombreConsumidor);
        sala.setAceptoGrabacionEn(OffsetDateTime.now());

        Sala salaActualizada = salaRepository.save(sala);
        return salaMapper.toDto(salaActualizada);
    }

    /**
     * Método privado para generar un token.
     */
    private String generarTokenUnico() {
        String token;
        do {
            // Genera un token aleatorio, quitando los guiones del UUID
            token = UUID.randomUUID().toString().replace("-", "");
        } while (salaRepository.existsByToken(token)); // Asegura que no exista ya en la BD
        return token;
    }
}

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

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaService {

    /** Días de vigencia de un enlace de sala (RNF-04). */
    private static final int DIAS_VIGENCIA_ENLACE = 7;

    /** Bytes aleatorios del token: 32 bytes = 256 bits. */
    private static final int BYTES_TOKEN = 32;

    private static final SecureRandom GENERADOR_SEGURO = new SecureRandom();

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
        // Precisión de microsegundos, igual que TIMESTAMPTZ en PostgreSQL.
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        sala.setEmitidaEn(ahora);

        // El vencimiento siempre es la emisión más 7 días (RNF-04, ck_salas_vencimiento);
        // se ignora cualquier valor que venga en el DTO.
        sala.setVenceEn(ahora.plusDays(DIAS_VIGENCIA_ENLACE));

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
            // 256 bits aleatorios en Base64URL sin relleno: 43 caracteres, no adivinable (diccionario SAL-04).
            byte[] bytes = new byte[BYTES_TOKEN];
            GENERADOR_SEGURO.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (salaRepository.existsByToken(token)); // Asegura que no exista ya en la BD
        return token;
    }
}

package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.FragmentoAckDTO;
import com.eduardo.vozclientebackend.dto.GrabacionDTO;
import com.eduardo.vozclientebackend.enums.CanalGrabacion;
import com.eduardo.vozclientebackend.enums.EstadoEntrevista;
import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.exceptions.FragmentoFueraDeOrdenException;
import com.eduardo.vozclientebackend.exceptions.GrabacionEstadoInvalidoException;
import com.eduardo.vozclientebackend.mappers.GrabacionMapper;
import com.eduardo.vozclientebackend.models.Grabacion;
import com.eduardo.vozclientebackend.repository.EntrevistaRepository;
import com.eduardo.vozclientebackend.repository.GrabacionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Recibe la grabación de una entrevista por fragmentos mientras la llamada sigue en curso (HU-10).
 *
 * <p>Ciclo de vida: {@link #iniciarGrabacion} (GRABANDO) → {@link #recibirFragmento} (cada ~5 s, por
 * canal) → {@link #finalizarGrabacion} (COMPLETADA, o SIN_CONTENIDO si no llegó nada). Si el cliente
 * desaparece sin finalizar, {@link #cerrarPorInactividad} cierra la grabación como parcial con lo que
 * ya estaba en disco, así una caída de red no pierde lo grabado.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrabacionService {

    /** Tamaño máximo por archivo (mismo límite que ck_grabaciones_tamano: 200 MB). */
    private static final long MAX_BYTES = 209_715_200L;

    private final GrabacionRepository grabacionRepository;
    private final EntrevistaRepository entrevistaRepository;
    private final EntrevistaService entrevistaService;
    private final GrabacionMapper grabacionMapper;
    private final TranscripcionService transcripcionService;

    // Se lee de app.storage.grabaciones.path (variable APP_STORAGE_GRABACIONES_PATH): en Docker es
    // /app/grabaciones, la única carpeta con permiso de escritura para el usuario del contenedor.
    @Value("${app.storage.grabaciones.path:./uploads/grabaciones}")
    private String baseStoragePath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(baseStoragePath));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el directorio de almacenamiento para las grabaciones", e);
        }
    }

    // ------------------------------------------------------------------
    // 1. Inicio
    // ------------------------------------------------------------------

    /**
     * Crea la grabación en estado GRABANDO con los dos archivos vacíos. Es idempotente: si ya
     * estaba GRABANDO (por ejemplo, un reintento de red) devuelve la existente.
     */
    @Transactional
    public GrabacionDTO iniciarGrabacion(Long entrevistaId) {
        if (!entrevistaRepository.existsById(entrevistaId)) {
            throw new IllegalArgumentException("Entrevista no encontrada con ID: " + entrevistaId);
        }

        Optional<Grabacion> existente = grabacionRepository.findByEntrevistaIdParaActualizar(entrevistaId);
        if (existente.isPresent()) {
            Grabacion g = existente.get();
            if (g.getEstado() == EstadoGrabacion.GRABANDO) {
                return grabacionMapper.toDto(g);
            }
            throw new GrabacionEstadoInvalidoException(
                "La entrevista " + entrevistaId + " ya tiene una grabación en estado " + g.getEstado() + ".");
        }

        Path cliente = rutaCanal(entrevistaId, CanalGrabacion.CLIENTE);
        Path representante = rutaCanal(entrevistaId, CanalGrabacion.REPRESENTANTE);
        try {
            Files.write(cliente, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.write(representante, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("No se pudieron crear los archivos de la entrevista {}", entrevistaId, e);
            throw new RuntimeException("Error al preparar el almacenamiento de la grabación", e);
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        Grabacion g = new Grabacion();
        g.setEntrevistaId(entrevistaId);
        g.setRutaCanalCliente(cliente.toString());
        g.setRutaCanalRepresentante(representante.toString());
        g.setFormato("webm");
        g.setTamanoBytes(0L);
        g.setDuracionSegundos(0);
        g.setHashSha256(null);
        g.setTieneAudioCliente(true);
        g.setEsParcial(false);
        g.setEstado(EstadoGrabacion.GRABANDO);
        g.setCreadoEn(ahora);
        g.setUltimoFragmentoEn(ahora);
        return grabacionMapper.toDto(grabacionRepository.save(g));
    }

    // ------------------------------------------------------------------
    // 2. Fragmentos
    // ------------------------------------------------------------------

    /**
     * Valida y anexa un fragmento al archivo del canal.
     *
     * <ul>
     *   <li>secuencia = última + 1: se acepta y se anexa.</li>
     *   <li>secuencia ≤ última: es un reenvío (se perdió la respuesta); se confirma sin anexar de nuevo.</li>
     *   <li>secuencia &gt; última + 1: falta uno; se rechaza con la secuencia esperada.</li>
     * </ul>
     *
     * @param hashSha256 huella SHA-256 (hex) del fragmento; opcional, si viene se verifica
     */
    @Transactional
    public FragmentoAckDTO recibirFragmento(Long entrevistaId, CanalGrabacion canal, int secuencia,
                                            MultipartFile fragmento, String hashSha256) {
        if (secuencia < 0) {
            throw new IllegalArgumentException("La secuencia no puede ser negativa.");
        }
        if (fragmento == null || fragmento.isEmpty()) {
            throw new IllegalArgumentException("El fragmento llegó vacío.");
        }

        Grabacion g = obtenerBloqueada(entrevistaId);
        if (g.getEstado() != EstadoGrabacion.GRABANDO) {
            throw new GrabacionEstadoInvalidoException(
                "La grabación de la entrevista " + entrevistaId + " ya no acepta fragmentos (estado "
                    + g.getEstado() + ").");
        }

        int ultima = ultimaSecuencia(g, canal);
        if (secuencia <= ultima) {
            return ack(g, canal); // reenvío ya procesado
        }
        if (secuencia != ultima + 1) {
            throw new FragmentoFueraDeOrdenException(ultima + 1);
        }

        try {
            byte[] bytes = fragmento.getBytes();

            if (hashSha256 != null && !hashSha256.isBlank()
                && !hashSha256.equalsIgnoreCase(sha256(bytes))) {
                throw new IllegalArgumentException("El fragmento " + secuencia + " llegó dañado (la huella no coincide).");
            }

            long confirmados = bytesConfirmados(g, canal);
            if (confirmados + bytes.length > MAX_BYTES) {
                throw new GrabacionEstadoInvalidoException(
                    "Se alcanzó el tamaño máximo de grabación (200 MB) en el canal " + canal + ".");
            }

            anexar(Paths.get(rutaDelCanal(g, canal)), confirmados, bytes);

            long nuevoTotal = confirmados + bytes.length;
            if (canal == CanalGrabacion.CLIENTE) {
                g.setTamanoBytes(nuevoTotal);
                g.setUltimaSecuenciaCliente(secuencia);
            } else {
                g.setBytesCanalRepresentante(nuevoTotal);
                g.setUltimaSecuenciaRepresentante(secuencia);
            }
            g.setUltimoFragmentoEn(OffsetDateTime.now());
            grabacionRepository.save(g);
            return ack(g, canal);

        } catch (IOException e) {
            log.error("Error al guardar el fragmento {} ({}) de la entrevista {}", secuencia, canal, entrevistaId, e);
            throw new RuntimeException("Error al almacenar el fragmento de la grabación", e);
        }
    }

    // ------------------------------------------------------------------
    // 3. Cierre
    // ------------------------------------------------------------------

    /**
     * Cierra la grabación: calcula la huella y deja el estado final. Es idempotente: si ya estaba
     * cerrada devuelve el estado actual sin cambios.
     */
    @Transactional
    public GrabacionDTO finalizarGrabacion(Long entrevistaId, Integer duracionSegundos,
                                           Boolean tieneAudioCliente, Boolean esParcial) {
        Grabacion g = obtenerBloqueada(entrevistaId);
        if (g.getEstado() != EstadoGrabacion.GRABANDO) {
            return grabacionMapper.toDto(g);
        }
        return cerrar(g,
            duracionSegundos == null ? 0 : duracionSegundos,
            tieneAudioCliente == null || tieneAudioCliente,
            Boolean.TRUE.equals(esParcial));
    }

    /**
     * IDs de entrevista con grabación GRABANDO cuyo último fragmento llegó hace más de la inactividad
     * indicada. Lo usa el planificador; cada cierre se hace en su propia transacción.
     */
    @Transactional(readOnly = true)
    public List<Long> entrevistasConGrabacionInactiva(Duration inactividad) {
        OffsetDateTime limite = OffsetDateTime.now().minus(inactividad);
        return grabacionRepository
            .findByEstadoAndUltimoFragmentoEnBefore(EstadoGrabacion.GRABANDO, limite)
            .stream()
            .map(Grabacion::getEntrevistaId)
            .toList();
    }

    /**
     * Cierra como parcial una grabación abandonada (el cliente se desconectó sin finalizar) y marca
     * la entrevista como FALLIDA si seguía EN_CURSO, igual que hace el front end cuando detecta el corte.
     */
    @Transactional
    public void cerrarPorInactividad(Long entrevistaId) {
        Grabacion g = obtenerBloqueada(entrevistaId);
        if (g.getEstado() != EstadoGrabacion.GRABANDO) {
            return; // otro proceso ya la cerró
        }

        OffsetDateTime ultimo = g.getUltimoFragmentoEn() != null ? g.getUltimoFragmentoEn() : g.getCreadoEn();
        int duracion = (int) Math.max(0, Duration.between(g.getCreadoEn(), ultimo).getSeconds());
        cerrar(g, duracion, g.getTieneAudioCliente(), true);
        log.warn("Grabación de la entrevista {} cerrada como parcial por inactividad.", entrevistaId);

        entrevistaRepository.findById(entrevistaId)
            .filter(e -> e.getEstado() == EstadoEntrevista.EN_CURSO)
            .ifPresent(e -> entrevistaService.reportarFallo(entrevistaId));
    }

    private GrabacionDTO cerrar(Grabacion g, int duracionSegundos, boolean tieneAudioCliente, boolean esParcial) {
        try {
            Path cliente = Paths.get(g.getRutaCanalCliente());
            long bytes = Files.exists(cliente) ? Files.size(cliente) : 0L;

            g.setTamanoBytes(bytes);
            g.setDuracionSegundos(duracionSegundos);
            g.setEsParcial(esParcial);

            if (bytes == 0) {
                g.setEstado(EstadoGrabacion.SIN_CONTENIDO);
                g.setTieneAudioCliente(false);
                g.setHashSha256(null);
            } else {
                g.setEstado(EstadoGrabacion.COMPLETADA);
                g.setTieneAudioCliente(tieneAudioCliente);
                g.setHashSha256(calcularHashSha256(cliente));
            }
            Grabacion guardada = grabacionRepository.save(g);
            if (guardada.getEstado() == EstadoGrabacion.COMPLETADA) {
                // Se encola después del commit; no falla el cierre si la transcripción no se puede pedir.
                transcripcionService.solicitarAutomatica(guardada.getId());
            }
            return grabacionMapper.toDto(guardada);
        } catch (IOException e) {
            log.error("Error al cerrar la grabación de la entrevista {}", g.getEntrevistaId(), e);
            throw new RuntimeException("Error al cerrar la grabación", e);
        }
    }

    // ------------------------------------------------------------------
    // Subida completa (compatibilidad con el front end actual)
    // ------------------------------------------------------------------

    /**
     * Recibe el archivo .webm completo. Se conserva como respaldo: si la grabación estaba GRABANDO
     * (el cliente envió fragmentos pero también tiene el archivo entero), el archivo completo
     * reemplaza al del canal del consumidor y la grabación se cierra.
     */
    @Transactional
    public GrabacionDTO crearYGuardarGrabacionWebm(Long entrevistaId, MultipartFile archivoWebm,
                                                   Integer duracionSegundos, Boolean tieneAudioCliente,
                                                   Boolean esParcial) {
        if (archivoWebm == null || archivoWebm.isEmpty()) {
            throw new IllegalArgumentException("El archivo de la grabación llegó vacío.");
        }

        Optional<Grabacion> existente = grabacionRepository.findByEntrevistaIdParaActualizar(entrevistaId);
        if (existente.isPresent() && existente.get().getEstado() != EstadoGrabacion.GRABANDO) {
            throw new GrabacionEstadoInvalidoException(
                "La entrevista con ID " + entrevistaId + " ya tiene una grabación asociada.");
        }

        try {
            Grabacion g = existente.orElseGet(Grabacion::new);
            Path destino = existente.isPresent()
                ? Paths.get(g.getRutaCanalCliente())
                : Paths.get(baseStoragePath).resolve(
                "entrevista_" + entrevistaId + "_" + System.currentTimeMillis() + ".webm");

            Files.copy(archivoWebm.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            g.setEntrevistaId(entrevistaId);
            g.setRutaCanalCliente(destino.toString());
            g.setFormato("webm");
            g.setTamanoBytes(archivoWebm.getSize());
            g.setDuracionSegundos(duracionSegundos);
            g.setHashSha256(calcularHashSha256(destino));
            g.setTieneAudioCliente(tieneAudioCliente);
            g.setEsParcial(esParcial);
            g.setEstado(EstadoGrabacion.COMPLETADA);
            if (g.getCreadoEn() == null) {
                g.setCreadoEn(OffsetDateTime.now());
            }
            Grabacion guardada = grabacionRepository.save(g);
            transcripcionService.solicitarAutomatica(guardada.getId());
            return grabacionMapper.toDto(guardada);

        } catch (IOException e) {
            log.error("Error al guardar el archivo de grabación webm para la entrevista {}", entrevistaId, e);
            throw new RuntimeException("Error al almacenar el archivo de audio/video", e);
        }
    }

    @Transactional(readOnly = true)
    public GrabacionDTO obtenerPorEntrevistaId(Long entrevistaId) {
        return grabacionRepository.findByEntrevistaId(entrevistaId)
            .map(grabacionMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Grabación no encontrada para la entrevista ID: " + entrevistaId));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Grabacion obtenerBloqueada(Long entrevistaId) {
        return grabacionRepository.findByEntrevistaIdParaActualizar(entrevistaId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No hay una grabación iniciada para la entrevista ID: " + entrevistaId));
    }

    private Path rutaCanal(Long entrevistaId, CanalGrabacion canal) {
        return Paths.get(baseStoragePath)
            .resolve("entrevista_" + entrevistaId + "_" + canal.name().toLowerCase() + ".webm");
    }

    private String rutaDelCanal(Grabacion g, CanalGrabacion canal) {
        return canal == CanalGrabacion.CLIENTE ? g.getRutaCanalCliente() : g.getRutaCanalRepresentante();
    }

    private int ultimaSecuencia(Grabacion g, CanalGrabacion canal) {
        return canal == CanalGrabacion.CLIENTE ? g.getUltimaSecuenciaCliente() : g.getUltimaSecuenciaRepresentante();
    }

    private long bytesConfirmados(Grabacion g, CanalGrabacion canal) {
        return canal == CanalGrabacion.CLIENTE ? g.getTamanoBytes() : g.getBytesCanalRepresentante();
    }

    private FragmentoAckDTO ack(Grabacion g, CanalGrabacion canal) {
        return new FragmentoAckDTO(g.getEntrevistaId(), canal, ultimaSecuencia(g, canal), bytesConfirmados(g, canal));
    }

    /**
     * Escribe el fragmento justo después de los bytes confirmados. Si un intento anterior alcanzó a
     * escribir en disco pero no a confirmarse en la base de datos, el archivo se recorta antes de escribir
     * y no queda contenido duplicado.
     */
    private void anexar(Path ruta, long bytesConfirmados, byte[] bytes) throws IOException {
        try (FileChannel canal = FileChannel.open(ruta, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            canal.truncate(bytesConfirmados);
            canal.position(bytesConfirmados);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                canal.write(buffer);
            }
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }

    /** SHA-256 de un archivo leído por bloques (eficiente para archivos pesados). */
    private String calcularHashSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }
}




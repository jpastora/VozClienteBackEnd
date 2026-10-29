package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.GrabacionDTO;
import com.eduardo.vozclientebackend.enums.EstadoGrabacion;
import com.eduardo.vozclientebackend.mappers.GrabacionMapper;
import com.eduardo.vozclientebackend.models.Grabacion;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrabacionService {

    private final GrabacionRepository grabacionRepository;
    private final GrabacionMapper grabacionMapper;

    // Ruta donde se guardarán los archivos .webm (puedes configurarlo en application.properties)
    // Ejemplo: app.storage.grabaciones.path=/var/data/grabaciones
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

    /**
     * Recibe el archivo .webm de WebRTC, lo guarda en disco y registra la entidad en la base de datos.
     * Nota: duracionSegundos y tieneAudioCliente usualmente se envían desde el frontend (JS)
     * ya que extraer metadatos de un webm en Java puro requiere librerías externas (ej. FFmpeg).
     */
    @Transactional
    public GrabacionDTO crearYGuardarGrabacionWebm(Long entrevistaId, MultipartFile archivoWebm,
                                                   Integer duracionSegundos, Boolean tieneAudioCliente,
                                                   Boolean esParcial) {

        // 1. Validar que la entrevista no tenga ya una grabación (Constraint UNIQUE)
        if (grabacionRepository.existsByEntrevistaId(entrevistaId)) {
            throw new IllegalStateException("La entrevista con ID " + entrevistaId + " ya tiene una grabación asociada.");
        }

        try {
            // 2. Generar nombre de archivo único y ruta
            String fileName = "entrevista_" + entrevistaId + "_" + System.currentTimeMillis() + ".webm";
            Path targetLocation = Paths.get(baseStoragePath).resolve(fileName);

            // 3. Guardar el archivo en el sistema
            Files.copy(archivoWebm.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 4. Calcular el Hash SHA-256 del archivo para asegurar integridad
            String hashSha256 = calcularHashSha256(targetLocation);

            // 5. Construir la entidad
            Grabacion grabacion = new Grabacion();
            grabacion.setEntrevistaId(entrevistaId);
            grabacion.setRutaCanalCliente(targetLocation.toString());
            // Si tuvieras otro archivo para el representante lo agregarías aquí, por ahora queda null o igual
            grabacion.setRutaCanalRepresentante(null);
            grabacion.setFormato("webm");
            grabacion.setTamanoBytes(archivoWebm.getSize());
            grabacion.setDuracionSegundos(duracionSegundos);
            grabacion.setHashSha256(hashSha256);
            grabacion.setTieneAudioCliente(tieneAudioCliente);
            grabacion.setEsParcial(esParcial);
            grabacion.setEstado(EstadoGrabacion.COMPLETADA);
            grabacion.setCreadoEn(OffsetDateTime.now());

            // 6. Guardar en base de datos
            Grabacion grabacionGuardada = grabacionRepository.save(grabacion);

            return grabacionMapper.toDto(grabacionGuardada);

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

    /**
     * Utilidad para calcular el SHA-256 leyendo el archivo por bloques
     * (Eficiente para archivos pesados de video/audio).
     */
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
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }
}

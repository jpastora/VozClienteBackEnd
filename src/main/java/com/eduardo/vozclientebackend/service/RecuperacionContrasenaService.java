package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.enums.EstadoCuenta;
import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.repository.CuentaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Recuperación de contraseña ("¿Olvidó su contraseña?").
 *
 * <p>Reutiliza la contraseña temporal: se genera una nueva, se envía por correo y, al ingresar con
 * ella, el sistema obliga a elegir una contraseña nueva.</p>
 *
 * <ul>
 *   <li>La respuesta es siempre la misma, exista o no el correo, para no revelar qué correos están
 *       registrados. El correo se envía en segundo plano, así que la respuesta tampoco tarda más
 *       cuando la cuenta existe.</li>
 *   <li>Solo aplica a cuentas activas o pendientes que ingresan con contraseña. Una cuenta vinculada
 *       a Google ingresa con Google y no recibe contraseña.</li>
 *   <li>Cada correo puede pedir como máximo {@value #MAXIMO_SOLICITUDES} recuperaciones cada
 *       {@link #VENTANA}; las demás se ignoran en silencio.</li>
 * </ul>
 */
@Service
public class RecuperacionContrasenaService {

    /** Respuesta única para cualquier solicitud. */
    public static final String RESPUESTA =
        "Si el correo está registrado, te enviamos una contraseña temporal. Revisa tu bandeja de entrada y la carpeta de spam.";

    /** Solicitudes permitidas por correo en la ventana. */
    static final int MAXIMO_SOLICITUDES = 3;

    /** Ventana en la que se cuentan las solicitudes. */
    static final Duration VENTANA = Duration.ofMinutes(30);

    /** A partir de este tamaño se eliminan los registros viejos, para que la memoria no crezca sin límite. */
    private static final int REGISTROS_ANTES_DE_LIMPIAR = 10_000;

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionContrasenaService.class);

    private final CuentaRepository cuentaRepository;
    private final ContrasenaTemporalService contrasenaTemporalService;
    private final CorreoService correoService;
    private final Clock reloj;
    private final Executor ejecutor;

    /** Solicitudes recientes por correo (en memoria; alcanza para una sola instancia del back-end). */
    private final Map<String, Deque<Instant>> solicitudes = new ConcurrentHashMap<>();

    @Autowired
    public RecuperacionContrasenaService(CuentaRepository cuentaRepository,
                                         ContrasenaTemporalService contrasenaTemporalService,
                                         CorreoService correoService,
                                         Clock reloj) {
        this(cuentaRepository, contrasenaTemporalService, correoService, reloj, ForkJoinPool.commonPool());
    }

    /** Constructor para las pruebas, que envían el correo en el mismo hilo. */
    RecuperacionContrasenaService(CuentaRepository cuentaRepository,
                                  ContrasenaTemporalService contrasenaTemporalService,
                                  CorreoService correoService,
                                  Clock reloj,
                                  Executor ejecutor) {
        this.cuentaRepository = cuentaRepository;
        this.contrasenaTemporalService = contrasenaTemporalService;
        this.correoService = correoService;
        this.reloj = reloj;
        this.ejecutor = ejecutor;
    }

    /**
     * Atiende una solicitud de recuperación. Nunca lanza un error que revele si el correo existe.
     *
     * @param correo correo escrito por la persona
     */
    @Transactional
    public void solicitar(String correo) {
        String normalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        if (normalizado.isEmpty() || !permitir(normalizado)) {
            return;
        }

        Cuenta cuenta = cuentaRepository.buscarPorCorreo(normalizado).orElse(null);
        if (cuenta == null || cuenta.getEstado() == EstadoCuenta.INACTIVA || cuenta.getGoogleSub() != null) {
            return;
        }

        String temporal = contrasenaTemporalService.asignar(cuenta);
        cuenta.setActualizadoEn(OffsetDateTime.now(reloj));
        cuentaRepository.save(cuenta);

        String destino = cuenta.getCorreo();
        String nombre = cuenta.getNombreCompleto();
        OffsetDateTime venceEn = cuenta.getContrasenaTemporalVenceEn();
        Runnable enviar = () -> {
            if (!correoService.enviarRecuperacionContrasena(destino, nombre, temporal, venceEn)) {
                LOG.warn("No se pudo enviar el correo de recuperación a la cuenta {}", cuenta.getId());
            }
        };

        // El correo sale solo si la contraseña nueva quedó guardada.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ejecutor.execute(enviar);
                }
            });
        } else {
            ejecutor.execute(enviar);
        }
    }

    /** Registra la solicitud y dice si el correo todavía puede pedir otra en la ventana. */
    boolean permitir(String correo) {
        Instant ahora = reloj.instant();
        Instant limite = ahora.minus(VENTANA);
        boolean[] permitido = {false};
        solicitudes.compute(correo, (clave, anteriores) -> {
            Deque<Instant> recientes = anteriores == null ? new ArrayDeque<>() : anteriores;
            while (!recientes.isEmpty() && !recientes.peekFirst().isAfter(limite)) {
                recientes.pollFirst();
            }
            if (recientes.size() < MAXIMO_SOLICITUDES) {
                recientes.addLast(ahora);
                permitido[0] = true;
            }
            return recientes;
        });
        if (solicitudes.size() > REGISTROS_ANTES_DE_LIMPIAR) {
            solicitudes.entrySet().removeIf(e -> e.getValue().isEmpty() || !e.getValue().peekLast().isAfter(limite));
        }
        return permitido[0];
    }
}

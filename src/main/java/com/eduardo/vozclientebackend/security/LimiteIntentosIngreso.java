package com.eduardo.vozclientebackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de ingreso con contraseña, para frenar ataques de fuerza bruta
 * (HU-01, RNF de seguridad).
 *
 * <ul>
 *   <li>Después de {@value #MAXIMO_FALLOS} fallos seguidos con el mismo correo en
 *       {@link #VENTANA}, el ingreso con ese correo queda bloqueado durante {@link #BLOQUEO}
 *       y responde 429.</li>
 *   <li>Cuenta también los fallos de correos que no existen, para que el bloqueo no revele qué
 *       correos están registrados.</li>
 *   <li>Un ingreso correcto borra el conteo.</li>
 * </ul>
 *
 * <p>El conteo vive en memoria: se reinicia si la aplicación se reinicia y no se comparte entre
 * varias instancias. Alcanza para una sola instancia del back-end; con varias habría que guardarlo
 * en la base de datos o en Redis.</p>
 */
@Component
public class LimiteIntentosIngreso {

    /** Fallos permitidos antes de bloquear. */
    public static final int MAXIMO_FALLOS = 5;

    /** Tiempo en el que se cuentan los fallos; pasado este tiempo sin bloqueo, el conteo vuelve a cero. */
    public static final Duration VENTANA = Duration.ofMinutes(15);

    /** Duración del bloqueo. */
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    public static final String MENSAJE =
        "Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo, o ingrese con Google";

    /** A partir de este tamaño se eliminan los registros vencidos, para que la memoria no crezca sin límite. */
    private static final int REGISTROS_ANTES_DE_LIMPIAR = 10_000;

    private record Registro(int fallos, Instant primerFallo, Instant bloqueadoHasta) {
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimiteIntentosIngreso(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Revisa si el correo puede intentar ingresar.
     *
     * @param correo correo normalizado
     * @throws ResponseStatusException 429 si el correo está bloqueado
     */
    public void verificar(String correo) {
        Registro registro = registros.get(correo);
        if (registro != null && registro.bloqueadoHasta() != null
            && reloj.instant().isBefore(registro.bloqueadoHasta())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, MENSAJE);
        }
    }

    /**
     * Suma un fallo al correo y lo bloquea si llegó al máximo.
     *
     * @param correo correo normalizado
     */
    public void registrarFallo(String correo) {
        Instant ahora = reloj.instant();
        registros.compute(correo, (clave, anterior) -> {
            boolean empiezaDeCero = anterior == null || vencido(anterior, ahora);
            int fallos = empiezaDeCero ? 1 : anterior.fallos() + 1;
            Instant primerFallo = empiezaDeCero ? ahora : anterior.primerFallo();
            Instant bloqueadoHasta = fallos >= MAXIMO_FALLOS ? ahora.plus(BLOQUEO) : null;
            return new Registro(fallos, primerFallo, bloqueadoHasta);
        });
        if (registros.size() > REGISTROS_ANTES_DE_LIMPIAR) {
            registros.values().removeIf(registro -> vencido(registro, ahora));
        }
    }

    /**
     * Borra el conteo del correo después de un ingreso correcto.
     *
     * @param correo correo normalizado
     */
    public void registrarExito(String correo) {
        registros.remove(correo);
    }

    private static boolean vencido(Registro registro, Instant ahora) {
        if (registro.bloqueadoHasta() != null) {
            return !ahora.isBefore(registro.bloqueadoHasta());
        }
        return !ahora.isBefore(registro.primerFallo().plus(VENTANA));
    }
}

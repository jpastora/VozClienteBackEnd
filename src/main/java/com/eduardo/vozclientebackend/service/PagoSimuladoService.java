package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.dto.ReglasNombre;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pasarela de pago simulada (RF-18: "el pago opera en modo de prueba o simulado, sin transacciones
 * reales").
 *
 * <ul>
 *   <li>Valida el formato de los datos de la tarjeta de prueba: titular, 16 dígitos con
 *       dígito verificador (algoritmo de Luhn), vencimiento MM/AA no vencido y CVV de 3 dígitos.</li>
 *   <li>Aprueba cualquier tarjeta válida, salvo las tarjetas de prueba de rechazo de
 *       {@link #TARJETAS_RECHAZADAS}, que permiten demostrar un pago rechazado.</li>
 *   <li>Nunca guarda ni registra en los logs el número completo ni el CVV: solo devuelve una
 *       referencia con los últimos 4 dígitos.</li>
 * </ul>
 */
@Service
public class PagoSimuladoService {

    /** Tarjeta de prueba que siempre se aprueba (se muestra como ejemplo en la pantalla). */
    public static final String TARJETA_APROBADA = "4242424242424242";

    /** Tarjetas de prueba que el banco simulado rechaza, con el motivo que se informa. */
    static final Map<String, String> TARJETAS_RECHAZADAS = Map.of(
        "4000000000000002", "Tarjeta rechazada por el banco emisor",
        "4000000000009995", "Fondos insuficientes");

    private static final Pattern NUMERO = Pattern.compile("^\\d{16}$");
    private static final Pattern VENCIMIENTO = Pattern.compile("^(0[1-9]|1[0-2])/(\\d{2})$");
    private static final Pattern CVV = Pattern.compile("^\\d{3}$");
    private static final Pattern NOMBRE = Pattern.compile(ReglasNombre.PATRON);
    private static final int LARGO_MINIMO_TITULAR = 2;
    private static final int LARGO_MAXIMO_TITULAR = 100;

    private final Clock reloj;

    public PagoSimuladoService(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Resultado del pago simulado.
     *
     * @param aprobado   true si el pago se aprobó
     * @param referencia referencia del pago, con los últimos 4 dígitos (y el motivo si se rechazó)
     * @param motivo     motivo del rechazo; null si se aprobó
     */
    public record Resultado(boolean aprobado, String referencia, String motivo) {
    }

    /**
     * Valida los datos de la tarjeta de prueba y decide si el pago se aprueba.
     *
     * @param titular       nombre del titular
     * @param numeroTarjeta número de la tarjeta (se admiten espacios y guiones)
     * @param vencimiento   MM/AA
     * @param cvv           3 dígitos
     * @param clave         clave de idempotencia de la compra, para armar la referencia
     * @return aprobado o rechazado, con su referencia
     * @throws ResponseStatusException 400 si algún dato no tiene un formato válido
     */
    public Resultado procesar(String titular, String numeroTarjeta, String vencimiento, String cvv, UUID clave) {
        validarTitular(titular);
        String numero = limpiarNumero(numeroTarjeta);
        validarVencimiento(vencimiento);
        if (cvv == null || !CVV.matcher(cvv.trim()).matches()) {
            throw invalido("El CVV debe tener 3 dígitos.");
        }

        String referencia = "SIM-" + clave.toString().substring(0, 8).toUpperCase(Locale.ROOT)
            + " · ****" + numero.substring(12);
        String motivo = TARJETAS_RECHAZADAS.get(numero);
        if (motivo != null) {
            return new Resultado(false, referencia + " · " + motivo, motivo);
        }
        return new Resultado(true, referencia, null);
    }

    private static void validarTitular(String titular) {
        String limpio = titular == null ? "" : titular.trim().replaceAll("\\s+", " ");
        if (limpio.length() < LARGO_MINIMO_TITULAR || limpio.length() > LARGO_MAXIMO_TITULAR
            || !NOMBRE.matcher(limpio).matches()) {
            throw invalido("El nombre del titular solo admite letras y espacios (de 2 a 100 caracteres).");
        }
    }

    private static String limpiarNumero(String numeroTarjeta) {
        String numero = numeroTarjeta == null ? "" : numeroTarjeta.replaceAll("[\\s-]", "");
        if (!NUMERO.matcher(numero).matches() || !cumpleLuhn(numero)) {
            throw invalido("El número de tarjeta no es válido. Usa una tarjeta de prueba de 16 dígitos.");
        }
        return numero;
    }

    private void validarVencimiento(String vencimiento) {
        Matcher m = vencimiento == null ? null : VENCIMIENTO.matcher(vencimiento.trim());
        if (m == null || !m.matches()) {
            throw invalido("El vencimiento debe tener el formato MM/AA.");
        }
        YearMonth tarjeta = YearMonth.of(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        if (tarjeta.isBefore(YearMonth.now(reloj))) {
            throw invalido("La tarjeta está vencida.");
        }
    }

    /** Dígito verificador de las tarjetas (algoritmo de Luhn). */
    static boolean cumpleLuhn(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

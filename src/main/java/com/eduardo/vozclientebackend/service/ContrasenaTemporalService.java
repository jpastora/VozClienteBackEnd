package com.eduardo.vozclientebackend.service;

import com.eduardo.vozclientebackend.models.Cuenta;
import com.eduardo.vozclientebackend.security.PoliticaContrasena;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Genera y asigna contraseñas temporales (cuentas creadas por un administrador).
 *
 * <p>La contraseña cumple {@link PoliticaContrasena}, vence a los 7 días y obliga a cambiarla en el
 * primer ingreso. Solo se guarda su hash; el texto plano se devuelve una vez para enviarlo por correo.</p>
 */
@Service
@RequiredArgsConstructor
public class ContrasenaTemporalService {

    /** Largo de la contraseña temporal. */
    static final int LARGO = 12;
    /** Vigencia de la contraseña temporal. */
    static final Duration VIGENCIA = Duration.ofDays(7);

    // Sin caracteres que se confunden al leerlos (0/O, 1/l/I).
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS;

    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    /**
     * Asigna una contraseña temporal nueva a la cuenta (no la guarda en la base de datos).
     *
     * @return la contraseña en texto plano, para enviarla por correo
     */
    public String asignar(Cuenta cuenta) {
        if (cuenta.getGoogleSub() != null) {
            // Una cuenta vinculada a Google no usa contraseña: poner una la obligaría a cambiarla al entrar.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La cuenta ingresa con Google y no usa contraseña");
        }
        String contrasena = generar();
        cuenta.setContrasenaHash(passwordEncoder.encode(contrasena));
        cuenta.setDebeCambiarContrasena(true);
        cuenta.setContrasenaTemporalVenceEn(OffsetDateTime.now(reloj).plus(VIGENCIA));
        return contrasena;
    }

    /** Genera una contraseña con al menos una mayúscula, una minúscula y un número. */
    String generar() {
        char[] c = new char[LARGO];
        c[0] = elegir(MAYUSCULAS);
        c[1] = elegir(MINUSCULAS);
        c[2] = elegir(NUMEROS);
        for (int i = 3; i < LARGO; i++) {
            c[i] = elegir(TODOS);
        }
        // Mezcla (Fisher-Yates) para que las tres primeras no estén siempre en la misma posición.
        for (int i = LARGO - 1; i > 0; i--) {
            int j = azar.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        String contrasena = new String(c);
        if (!PoliticaContrasena.cumple(contrasena)) {
            throw new IllegalStateException("La contraseña temporal generada no cumple la política");
        }
        return contrasena;
    }

    private char elegir(String conjunto) {
        return conjunto.charAt(azar.nextInt(conjunto.length()));
    }
}

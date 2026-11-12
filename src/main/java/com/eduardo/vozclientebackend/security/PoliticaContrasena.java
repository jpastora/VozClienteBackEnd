package com.eduardo.vozclientebackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de las contraseñas propias de la aplicación.
 *
 * <ul>
 *   <li>Entre 8 y 72 caracteres (72 bytes es el máximo que procesa BCrypt).</li>
 *   <li>Al menos una letra y un número.</li>
 * </ul>
 */
public final class PoliticaContrasena {

    /** Largo mínimo de la contraseña. */
    public static final int LARGO_MINIMO = 8;

    /** Largo máximo de la contraseña (límite de BCrypt). */
    public static final int LARGO_MAXIMO = 72;

    /** Mensaje con las reglas, para mostrar al usuario. */
    public static final String MENSAJE =
        "La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un número";

    private PoliticaContrasena() {
    }

    /**
     * Valida una contraseña nueva.
     *
     * @param contrasena contraseña en texto plano
     * @throws ResponseStatusException 400 si no cumple las reglas
     */
    public static void validar(String contrasena) {
        if (!cumple(contrasena)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MENSAJE);
        }
    }

    /**
     * Indica si una contraseña cumple las reglas.
     *
     * @param contrasena contraseña en texto plano
     * @return {@code true} si cumple
     */
    public static boolean cumple(String contrasena) {
        if (contrasena == null
            || contrasena.length() < LARGO_MINIMO
            || contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > LARGO_MAXIMO) {
            return false;
        }
        boolean tieneLetra = contrasena.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = contrasena.chars().anyMatch(Character::isDigit);
        return tieneLetra && tieneNumero;
    }
}

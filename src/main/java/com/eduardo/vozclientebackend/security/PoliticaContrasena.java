package com.eduardo.vozclientebackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * Reglas de las contraseñas propias de la aplicación.
 *
 * <ul>
 *   <li>Entre 8 y 20 caracteres.</li>
 *   <li>Al menos una mayúscula, una minúscula y un número.</li>
 * </ul>
 */
public final class PoliticaContrasena {

    /** Largo mínimo de la contraseña. */
    public static final int LARGO_MINIMO = 8;

    /** Largo máximo de la contraseña. */
    public static final int LARGO_MAXIMO = 20;

    /** Máximo de bytes que procesa BCrypt; con 20 caracteres solo se alcanza con símbolos poco comunes. */
    private static final int BYTES_MAXIMOS_BCRYPT = 72;

    /** Mensaje con las reglas, para mostrar al usuario. */
    public static final String MENSAJE =
        "La contraseña debe tener entre 8 y 20 caracteres, con al menos una mayúscula, una minúscula y un número";

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
            || contrasena.length() > LARGO_MAXIMO
            || contrasena.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_BCRYPT) {
            return false;
        }
        boolean tieneMayuscula = contrasena.chars().anyMatch(Character::isUpperCase);
        boolean tieneMinuscula = contrasena.chars().anyMatch(Character::isLowerCase);
        boolean tieneNumero = contrasena.chars().anyMatch(Character::isDigit);
        return tieneMayuscula && tieneMinuscula && tieneNumero;
    }
}

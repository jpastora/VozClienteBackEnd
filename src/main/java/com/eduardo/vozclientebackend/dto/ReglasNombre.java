package com.eduardo.vozclientebackend.dto;

/**
 * Regla del nombre completo de una cuenta: solo letras (con tildes, ñ y ü) separadas por un espacio,
 * sin números ni caracteres especiales.
 */
public final class ReglasNombre {

    /** Expresión regular para {@code @Pattern}: palabras de letras separadas por un solo espacio. */
    public static final String PATRON = "^\\p{L}+( \\p{L}+)*$";

    /** Mensaje de error de la regla. */
    public static final String MENSAJE = "El nombre solo puede tener letras y espacios, sin números ni caracteres especiales";

    private ReglasNombre() {
    }
}

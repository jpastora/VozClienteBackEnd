package com.eduardo.vozclientebackend.security;

import com.eduardo.vozclientebackend.dto.ReglasNombre;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de las reglas de contraseña y de nombre.
 */
class PoliticaContrasenaTest {

    @Test
    void cumple_mayusculaMinusculaYNumeroEntre8y20_devuelveVerdadero() {
        assertThat(PoliticaContrasena.cumple("Clave2026")).isTrue();
        assertThat(PoliticaContrasena.cumple("Abcdefg1")).isTrue();
        assertThat(PoliticaContrasena.cumple("Abcdefghijklmnopqr12")).isTrue();
    }

    @Test
    void cumple_faltaAlgunTipoDeCaracter_devuelveFalso() {
        assertThat(PoliticaContrasena.cumple("clave2026")).as("sin mayúscula").isFalse();
        assertThat(PoliticaContrasena.cumple("CLAVE2026")).as("sin minúscula").isFalse();
        assertThat(PoliticaContrasena.cumple("ClaveSegura")).as("sin número").isFalse();
    }

    @Test
    void cumple_largoFueraDeRango_devuelveFalso() {
        assertThat(PoliticaContrasena.cumple("Clave1")).as("menos de 8").isFalse();
        assertThat(PoliticaContrasena.cumple("Abcdefghijklmnopqr123")).as("más de 20").isFalse();
        assertThat(PoliticaContrasena.cumple(null)).isFalse();
    }

    @Test
    void patronNombre_soloLetrasYEspacios_aceptaTildesYRechazaNumerosYSimbolos() {
        assertThat("María José Núñez").matches(ReglasNombre.PATRON);
        assertThat("Ana").matches(ReglasNombre.PATRON);
        assertThat("Ana2").doesNotMatch(ReglasNombre.PATRON);
        assertThat("Ana_Vargas").doesNotMatch(ReglasNombre.PATRON);
        assertThat("Ana  Vargas").as("dos espacios seguidos").doesNotMatch(ReglasNombre.PATRON);
    }
}

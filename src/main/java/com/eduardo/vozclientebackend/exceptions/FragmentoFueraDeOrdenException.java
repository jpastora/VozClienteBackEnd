package com.eduardo.vozclientebackend.exceptions;

public class FragmentoFueraDeOrdenException extends RuntimeException {
    private final int secuenciaEsperada;

    public FragmentoFueraDeOrdenException(int secuenciaEsperada) {
        super("Fragmento fuera de orden. Se esperaba la secuencia " + secuenciaEsperada + ".");
        this.secuenciaEsperada = secuenciaEsperada;
    }

    public int getSecuenciaEsperada() {
        return secuenciaEsperada;
    }
}

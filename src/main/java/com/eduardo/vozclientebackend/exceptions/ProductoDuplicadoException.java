package com.eduardo.vozclientebackend.exceptions;

public class ProductoDuplicadoException
    extends RuntimeException {

    public ProductoDuplicadoException(String nombre) {
        super(
            "Ya existe un producto con el nombre '"
                + nombre
                + "' en esta empresa."
        );
    }
}

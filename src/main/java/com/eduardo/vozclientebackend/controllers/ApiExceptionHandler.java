package com.eduardo.vozclientebackend.controllers;

import com.eduardo.vozclientebackend.exceptions.FragmentoFueraDeOrdenException;
import com.eduardo.vozclientebackend.exceptions.GrabacionEstadoInvalidoException;
import com.eduardo.vozclientebackend.exceptions.SalaNoEliminableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import com.eduardo.vozclientebackend.exceptions.ProductoDuplicadoException;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Maneja errores de validación de los DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
        MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errores.put(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        Map<String, Object> respuesta = new HashMap<>();

        respuesta.put("mensaje", "Error de validación");
        respuesta.put("errores", errores);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(respuesta);
    }

    /**
     * Maneja conflictos de concurrencia optimista.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> manejarConcurrencia(
        ObjectOptimisticLockingFailureException ex) {

        Map<String, Object> respuesta = new HashMap<>();

        respuesta.put(
            "mensaje",
            "La información fue modificada por otro usuario"
        );

        respuesta.put(
            "detalle",
            "Actualice los datos e intente nuevamente"
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(respuesta);
    }


    @ExceptionHandler(ProductoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarDuplicado(
        ProductoDuplicadoException ex) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(SalaNoEliminableException.class)
    public ResponseEntity<Map<String, Object>> manejarSalaNoEliminable(
        SalaNoEliminableException ex) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("mensaje", ex.getMessage()));
    }


    @ExceptionHandler(FragmentoFueraDeOrdenException.class)
    public ResponseEntity<Map<String, Object>> manejarFragmentoFueraDeOrden(
        FragmentoFueraDeOrdenException ex) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("mensaje", ex.getMessage(), "secuenciaEsperada", ex.getSecuenciaEsperada()));
    }


    @ExceptionHandler(GrabacionEstadoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> manejarGrabacionEstadoInvalido(
        GrabacionEstadoInvalidoException ex) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumento(
        IllegalArgumentException ex) {

        return ResponseEntity
            .badRequest()
            .body(Map.of("mensaje", ex.getMessage()));
    }

}

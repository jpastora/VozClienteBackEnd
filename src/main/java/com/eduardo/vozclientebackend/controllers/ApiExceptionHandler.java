package com.eduardo.vozclientebackend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

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
}

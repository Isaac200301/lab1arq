package com.udea.lab1arq.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduce las excepciones de la capa de servicio a respuestas HTTP con sentido.
 *
 * Sin esto, un IllegalArgumentException se convierte en un 500 Internal Server
 * Error, que le dice al cliente "el servidor se rompio" cuando en realidad el
 * problema fue del cliente. Con esto el frontend recibe un 400 y un mensaje
 * legible que puede mostrarle al usuario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}

package com.banking.customer.exception;

/**
 * Conflicto con el estado actual del recurso → HTTP 409.
 * Ejemplos: documento duplicado, cliente con productos activos.
 */
public class CustomerConflictException extends RuntimeException {

    public CustomerConflictException(String message) {
        super(message);
    }
}

package com.banking.customer.exception;

/**
 * Excepción de dominio: recurso no encontrado → HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(java.util.UUID customerId) {
        super("Customer not found with id: " + customerId);
    }
}

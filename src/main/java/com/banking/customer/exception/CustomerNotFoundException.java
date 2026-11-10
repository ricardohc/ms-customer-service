package com.banking.customer.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(java.util.UUID customerId) {
        super("Customer not found with id: " + customerId);
    }
}

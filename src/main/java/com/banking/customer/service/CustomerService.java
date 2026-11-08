package com.banking.customer.service;

import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reglas de negocio del dominio Customer (ver docs/skill-customer.md).
 */
public interface CustomerService {

    Mono<CustomerResponse> create(CustomerCreateRequest request, String correlationId);

    Mono<CustomerResponse> findById(UUID customerId);

    Mono<CustomerPage> findAll(int page, int size);

    Mono<CustomerResponse> update(UUID customerId, CustomerUpdateRequest request, String correlationId);

    Mono<Void> deactivate(UUID customerId, String correlationId);
}

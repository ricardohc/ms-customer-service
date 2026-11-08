package com.banking.customer.controller;

import com.banking.customer.api.CustomerApi;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import com.banking.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implementación del contrato generado (CustomerApi). Delegación delgada al
 * servicio; el camino es 100% reactivo (Mono), sin block().
 */
@RestController
@RequiredArgsConstructor
public class CustomerApiImpl implements CustomerApi {

    private final CustomerService customerService;

    @Override
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            String xCorrelationID, String xChannelID, UUID xTransactionID,
            Mono<CustomerCreateRequest> customerCreateRequest, ServerWebExchange exchange) {
        return customerCreateRequest
                .flatMap(request -> customerService.create(request, xCorrelationID))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Override
    public Mono<ResponseEntity<Void>> deactivateCustomer(
            UUID customerId, String xCorrelationID, String xChannelID,
            UUID xTransactionID, ServerWebExchange exchange) {
        return customerService.deactivate(customerId, xCorrelationID)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @Override
    public Mono<ResponseEntity<CustomerPage>> findAllCustomers(
            String xCorrelationID, String xChannelID, Integer page, Integer size,
            ServerWebExchange exchange) {
        return customerService.findAll(page, size)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> findCustomerById(
            UUID customerId, String xCorrelationID, String xChannelID,
            ServerWebExchange exchange) {
        return customerService.findById(customerId)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            UUID customerId, String xCorrelationID, String xChannelID,
            UUID xTransactionID, Mono<CustomerUpdateRequest> customerUpdateRequest,
            ServerWebExchange exchange) {
        return customerUpdateRequest
                .flatMap(request -> customerService.update(customerId, request, xCorrelationID))
                .map(ResponseEntity::ok);
    }
}

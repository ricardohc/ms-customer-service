package com.banking.customer.controller;

import com.banking.customer.api.CustomerApi;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
public class CustomerApiImpl implements CustomerApi {

    @Override
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            String xCorrelationID, String xChannelID, UUID xTransactionID,
            Mono<CustomerCreateRequest> customerCreateRequest, ServerWebExchange exchange) {

        return customerCreateRequest.map(req -> {
            CustomerResponse response = new CustomerResponse();
            response.setId(UUID.randomUUID());
            response.setCustomerType(req.getCustomerType());
            response.setDocumentType(req.getDocumentType());
            response.setDocumentNumber(req.getDocumentNumber());
            response.setFirstName(req.getFirstName());
            response.setLastName(req.getLastName());
            response.setEmail(req.getEmail());
            response.setPhoneNumber(req.getPhoneNumber());
            response.setProfile(req.getProfile());
            response.setStatus(com.banking.customer.dto.CustomerStatus.ACTIVE);
            response.setCreatedAt(java.time.OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        });
    }

    @Override
    public Mono<ResponseEntity<Void>> deactivateCustomer(
            UUID customerId, String xCorrelationID, String xChannelID,
            UUID xTransactionID, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<CustomerPage>> findAllCustomers(
            String xCorrelationID, String xChannelID, Integer page, Integer size,
            ServerWebExchange exchange) {
        CustomerPage customerPage = new CustomerPage();
        customerPage.setContent(List.of());
        customerPage.setPage(page);
        customerPage.setSize(size);
        customerPage.setTotalElements(0);
        return Mono.just(ResponseEntity.ok(customerPage));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> findCustomerById(
            UUID customerId, String xCorrelationID, String xChannelID,
            ServerWebExchange exchange) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customerId);
        response.setFirstName("Juan");
        response.setLastName("Perez");
        response.setEmail("juan.perez@example.com");
        response.setStatus(com.banking.customer.dto.CustomerStatus.ACTIVE);
        return Mono.just(ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            UUID customerId, String xCorrelationID, String xChannelID,
            UUID xTransactionID, Mono<CustomerUpdateRequest> customerUpdateRequest,
            ServerWebExchange exchange) {
        return customerUpdateRequest.map(req -> {
            CustomerResponse response = new CustomerResponse();
            response.setId(customerId);
            response.setFirstName(req.getFirstName());
            response.setLastName(req.getLastName());
            response.setEmail(req.getEmail());
            response.setPhoneNumber(req.getPhoneNumber());
            response.setProfile(req.getProfile());
            response.setStatus(req.getStatus());
            return ResponseEntity.ok(response);
        });
    }
}
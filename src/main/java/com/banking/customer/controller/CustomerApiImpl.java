package com.banking.customer.controller;

import com.banking.customer.api.CustomerApi;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import com.banking.customer.service.CustomerService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public class CustomerApiImpl implements CustomerApi {

    private final CustomerService customerService;

    @Override
    public CompletableFuture<ResponseEntity<CustomerResponse>> createCustomer(
            String xCorrelationID,
            String xChannelID,
            UUID xTransactionID,
            CustomerCreateRequest customerCreateRequest) {
        return customerService.create(customerCreateRequest, xCorrelationID)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .toCompletionStage()
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<ResponseEntity<Void>> deactivateCustomer(
            UUID customerId,
            String xCorrelationID,
            String xChannelID,
            UUID xTransactionID) {
        return customerService.deactivate(customerId, xCorrelationID)
                .andThen(Single.just(ResponseEntity.noContent().<Void>build()))
                .toCompletionStage()
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<ResponseEntity<CustomerPage>> findAllCustomers(
            String xCorrelationID,
            String xChannelID,
            Integer page,
            Integer size) {
        return customerService.findAll(page, size)
                .map(ResponseEntity::ok)
                .toCompletionStage()
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<ResponseEntity<CustomerResponse>> findCustomerById(
            UUID customerId,
            String xCorrelationID,
            String xChannelID) {
        return customerService.findById(customerId)
                .map(ResponseEntity::ok)
                .toCompletionStage()
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<ResponseEntity<CustomerResponse>> updateCustomer(
            UUID customerId,
            String xCorrelationID,
            String xChannelID,
            UUID xTransactionID,
            CustomerUpdateRequest customerUpdateRequest) {
        return customerService.update(customerId, customerUpdateRequest, xCorrelationID)
                .map(ResponseEntity::ok)
                .toCompletionStage()
                .toCompletableFuture();
    }
}
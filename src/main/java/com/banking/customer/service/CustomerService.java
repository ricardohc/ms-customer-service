package com.banking.customer.service;

import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import java.util.UUID;

public interface CustomerService {

    Single<CustomerResponse> create(CustomerCreateRequest request, String correlationId);

    Single<CustomerResponse> findById(UUID customerId);

    Single<CustomerPage> findAll(int page, int size);

    Single<CustomerResponse> update(UUID customerId, CustomerUpdateRequest request, String correlationId);

    Completable deactivate(UUID customerId, String correlationId);
}
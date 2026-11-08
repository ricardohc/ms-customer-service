package com.banking.customer.service.impl;

import com.banking.customer.document.CustomerDocument;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerStatus;
import com.banking.customer.dto.CustomerUpdateRequest;
import com.banking.customer.event.CustomerEventProducer;
import com.banking.customer.exception.CustomerConflictException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.mapper.CustomerMapper;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementación reactiva de las reglas de negocio del dominio Customer.
 *
 * Reglas (docs/skill-customer.md):
 * - POST: unicidad de documentType + documentNumber → 409 si duplicado.
 * - PATCH: solo datos de contacto y perfil (el DTO no permite cambiar tipo/documento).
 * - DELETE: baja lógica (status = INACTIVE); la validación de productos activos
 *   se coordina vía ms-account-service (fuera del alcance de este servicio).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CustomerEventProducer eventProducer;

    @Override
    public Mono<CustomerResponse> create(CustomerCreateRequest request, String correlationId) {
        return repository.findByDocumentTypeAndDocumentNumber(
                        request.getDocumentType().getValue(), request.getDocumentNumber())
                .flatMap(existing -> Mono.<CustomerDocument>error(
                        new CustomerConflictException("Customer with document already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    CustomerDocument document = mapper.toDocument(request);
                    document.setId(UUID.randomUUID());
                    document.setStatus(CustomerStatus.ACTIVE.getValue());
                    document.setCreatedAt(Instant.now());
                    return repository.save(document);
                }))
                .map(saved -> {
                    log.info("Customer created id={} correlationId={}", saved.getId(), correlationId);
                    eventProducer.publishCreated(saved, correlationId);
                    return mapper.toResponse(saved);
                });
    }

    @Override
    public Mono<CustomerResponse> findById(UUID customerId) {
        return repository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .map(mapper::toResponse);
    }

    @Override
    public Mono<CustomerPage> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Mono<List<CustomerResponse>> content = repository.findAllBy(pageable)
                .map(mapper::toResponse)
                .collectList();
        Mono<Long> total = repository.countBy();
        return Mono.zip(content, total)
                .map(tuple -> {
                    CustomerPage result = new CustomerPage();
                    result.setContent(tuple.getT1());
                    result.setPage(page);
                    result.setSize(size);
                    result.setTotalElements(tuple.getT2().intValue());
                    return result;
                });
    }

    @Override
    public Mono<CustomerResponse> update(UUID customerId, CustomerUpdateRequest request, String correlationId) {
        return repository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(document -> {
                    mapper.applyUpdate(document, request);
                    return repository.save(document);
                })
                .map(saved -> {
                    log.info("Customer updated id={} correlationId={}", customerId, correlationId);
                    eventProducer.publishUpdated(saved, correlationId);
                    return mapper.toResponse(saved);
                });
    }

    @Override
    public Mono<Void> deactivate(UUID customerId, String correlationId) {
        return repository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(document -> {
                    if (CustomerStatus.INACTIVE.getValue().equals(document.getStatus())) {
                        return Mono.error(new CustomerConflictException("Customer is already inactive"));
                    }
                    document.setStatus(CustomerStatus.INACTIVE.getValue());
                    return repository.save(document).map(saved -> {
                        log.info("Customer deactivated id={} correlationId={}", customerId, correlationId);
                        eventProducer.publishDeactivated(saved, correlationId);
                        return saved;
                    });
                })
                .then();
    }
}

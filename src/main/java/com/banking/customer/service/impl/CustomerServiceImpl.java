package com.banking.customer.service.impl;

import com.banking.customer.document.CustomerDocument;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerStatus;
import com.banking.customer.dto.CustomerUpdateRequest;
import com.banking.customer.event.CustomerEventProducer;
import com.banking.customer.event.dto.CustomerEvent;
import com.banking.customer.exception.CustomerConflictException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.mapper.CustomerMapper;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.service.CustomerService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> PERSONAL_DOCUMENT_TYPES = Set.of("DNI", "CEX", "PASSPORT");
    private static final Set<String> BUSINESS_DOCUMENT_TYPES = Set.of("RUC");
    private static final Set<String> SUPPORTED_DOCUMENT_TYPES = Set.of("DNI", "CEX", "PASSPORT", "RUC");
    private static final String PERSONAL_PROFILE = "VIP";
    private static final String BUSINESS_PROFILE = "PYME";

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final CustomerEventProducer eventProducer;

    @Override
    public Single<CustomerResponse> create(CustomerCreateRequest request, String correlationId) {
        return Single.defer(() -> {
            String customerType = request.getCustomerType() != null ? request.getCustomerType().getValue() : null;
            String documentType = request.getDocumentType() != null ? request.getDocumentType().getValue() : null;
            String profile = request.getProfile() != null ? request.getProfile().getValue() : null;

            if (customerType == null
                    || (!"PERSONAL".equals(customerType) && !"BUSINESS".equals(customerType))) {
                return Single.error(new CustomerConflictException(
                        "Unsupported customer type. customerType must be one of PERSONAL, BUSINESS"));
            }

            boolean isBusiness = "BUSINESS".equals(customerType);
            Set<String> allowed = isBusiness ? BUSINESS_DOCUMENT_TYPES : PERSONAL_DOCUMENT_TYPES;
            String requiredProfile = isBusiness ? BUSINESS_PROFILE : PERSONAL_PROFILE;

            if (documentType == null || !SUPPORTED_DOCUMENT_TYPES.contains(documentType)) {
                return Single.error(new CustomerConflictException(
                        "Unsupported document type '" + documentType
                                + "'. documentType must be one of DNI, CEX, PASSPORT, RUC"));
            }
            if (!allowed.contains(documentType)) {
                return Single.error(new CustomerConflictException(
                        "Document type '" + documentType + "' is not allowed for customer type "
                                + customerType + ". Allowed: " + allowed));
            }
            if (profile == null || !requiredProfile.equals(profile)) {
                return Single.error(new CustomerConflictException(
                        "Unsupported profile '" + profile + "' for customer type " + customerType
                                + ". Profile must be " + requiredProfile));
            }

            return repository.findByDocumentTypeAndDocumentNumber(documentType, request.getDocumentNumber())
                    .flatMapSingle(found -> Single.<CustomerDocument>error(
                            new CustomerConflictException("Customer with document already exists")))
                    .switchIfEmpty(Single.defer(() -> {
                        CustomerDocument document = mapper.toDocument(request);
                        document.setId(UUID.randomUUID());
                        document.setStatus(CustomerStatus.ACTIVE.getValue());
                        document.setCreatedAt(Instant.now());
                        return repository.save(document);
                    }))
                    .map(saved -> {
                        log.info("Customer created id={} correlationId={}", saved.getId(), correlationId);
                        eventProducer.publish(
                                CustomerEvent.of(CustomerEvent.Type.CustomerCreated, saved, correlationId));
                        return mapper.toResponse(saved);
                    });
        });
    }

    @Override
    public Single<CustomerResponse> findById(UUID customerId) {
        return repository.findById(customerId)
                .switchIfEmpty(Single.error(new CustomerNotFoundException(customerId)))
                .map(mapper::toResponse);
    }

    @Override
    public Single<CustomerPage> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Single<List<CustomerResponse>> content = repository.findAllBy(pageable)
                .map(mapper::toResponse)
                .toList();
        Single<Long> total = repository.countBy();
        return Single.zip(content, total, (list, count) -> {
            CustomerPage result = new CustomerPage();
            result.setContent(list);
            result.setPage(page);
            result.setSize(size);
            result.setTotalElements(count.intValue());
            return result;
        });
    }

    @Override
    public Single<CustomerResponse> update(UUID customerId, CustomerUpdateRequest request, String correlationId) {
        if (request.getCustomerType() != null) {
            return Single.error(new CustomerConflictException("Changing customerType is not allowed."));
        }
        return repository.findById(customerId)
                .switchIfEmpty(Single.error(new CustomerNotFoundException(customerId)))
                .flatMap(document -> {
                    mapper.applyUpdate(document, request);
                    return repository.save(document);
                })
                .map(saved -> {
                    log.info("Customer updated id={} correlationId={}", customerId, correlationId);
                    eventProducer.publish(
                            CustomerEvent.of(CustomerEvent.Type.CustomerUpdated, saved, correlationId));
                    return mapper.toResponse(saved);
                });
    }

    @Override
    public Completable deactivate(UUID customerId, String correlationId) {
        return repository.findById(customerId)
                .switchIfEmpty(Single.error(new CustomerNotFoundException(customerId)))
                .flatMapCompletable(document -> {
                    if (CustomerStatus.INACTIVE.getValue().equals(document.getStatus())) {
                        return Completable.error(new CustomerConflictException("Customer is already inactive"));
                    }
                    document.setStatus(CustomerStatus.INACTIVE.getValue());
                    return repository.save(document)
                            .doOnSuccess(saved -> {
                                log.info("Customer deactivated id={} correlationId={}", customerId, correlationId);
                                eventProducer.publish(
                                        CustomerEvent.of(CustomerEvent.Type.CustomerDeactivated, saved, correlationId));
                            })
                            .ignoreElement();
                });
    }
}
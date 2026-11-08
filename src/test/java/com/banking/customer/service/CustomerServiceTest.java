package com.banking.customer.service;

import com.banking.customer.document.CustomerDocument;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerProfile;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerType;
import com.banking.customer.dto.DocumentType;
import com.banking.customer.event.CustomerEventProducer;
import com.banking.customer.exception.CustomerConflictException;
import com.banking.customer.mapper.CustomerMapper;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de reglas de negocio (docs/skill-customer.md):
 * - Duplicado de documento → 409 (CustomerConflictException).
 * - Asignar perfil VIP/PYME sin tarjeta de crédito → permitido en este servicio.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerEventProducer eventProducer;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(repository, new CustomerMapper(), eventProducer);
    }

    private CustomerCreateRequest validRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCustomerType(CustomerType.PERSONAL);
        request.setDocumentType(DocumentType.DNI);
        request.setDocumentNumber("12345678");
        request.setFirstName("Juan");
        request.setLastName("Perez");
        request.setEmail("juan@example.com");
        request.setProfile(CustomerProfile.VIP);
        return request;
    }

    @Test
    void createValidPersonalCustomerReturns201Equivalent() {
        when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                .thenReturn(Mono.empty());
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(validRequest(), "corr-1"))
                .assertNext(response -> {
                    assertThat(response.getDocumentNumber()).isEqualTo("12345678");
                    assertThat(response.getProfile()).isEqualTo(CustomerProfile.VIP);
                })
                .verifyComplete();
    }

    @Test
    void createDuplicateDocumentFailsWithConflict() {
        CustomerDocument existing = CustomerDocument.builder()
                .id(UUID.randomUUID())
                .documentType("DNI")
                .documentNumber("12345678")
                .build();
        when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(service.create(validRequest(), "corr-1"))
                .expectError(CustomerConflictException.class)
                .verify();
    }

    @Test
    void assignVipProfileWithoutCreditCardIsAllowedHere() {
        CustomerCreateRequest request = validRequest();
        when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                .thenReturn(Mono.empty());
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(request, "corr-1"))
                .assertNext(response ->
                        assertThat(response.getProfile()).isEqualTo(CustomerProfile.VIP))
                .verifyComplete();
    }

    @Test
    void findByIdNotFoundFails() {
        when(repository.findById(any(UUID.class))).thenReturn(Mono.empty());
        StepVerifier.create(service.findById(UUID.randomUUID()))
                .expectError(com.banking.customer.exception.CustomerNotFoundException.class)
                .verify();
    }
}

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
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de reglas de negocio (docs/skill-customer.md):
 * - Duplicado de documento → 409 (CustomerConflictException).
 * - Asignar perfil VIP/PYME sin tarjeta de crédito → permitido en este servicio.
 *
 * Migradas de StepVerifier (Reactor) a TestObserver (RxJava 3).
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
                .thenReturn(Maybe.empty());
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        TestObserver<CustomerResponse> observer = service.create(validRequest(), "corr-1").test();
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values().get(0).getDocumentNumber()).isEqualTo("12345678");
        assertThat(observer.values().get(0).getProfile()).isEqualTo(CustomerProfile.VIP);
    }

    @Test
    void createDuplicateDocumentFailsWithConflict() {
        CustomerDocument existing = CustomerDocument.builder()
                .id(UUID.randomUUID())
                .documentType("DNI")
                .documentNumber("12345678")
                .build();
        when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                .thenReturn(Maybe.just(existing));

        TestObserver<CustomerResponse> observer = service.create(validRequest(), "corr-1").test();
        observer.assertError(CustomerConflictException.class);
    }

    @Test
    void assignVipProfileWithoutCreditCardIsAllowedHere() {
        CustomerCreateRequest request = validRequest();
        when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                .thenReturn(Maybe.empty());
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        TestObserver<CustomerResponse> observer = service.create(request, "corr-1").test();
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values().get(0).getProfile()).isEqualTo(CustomerProfile.VIP);
    }

    @Test
    void createBusinessCustomerWithRucIsAllowed() {
        CustomerCreateRequest request = validRequest();
        request.setCustomerType(CustomerType.BUSINESS);
        request.setDocumentType(DocumentType.RUC);
        request.setProfile(CustomerProfile.PYME);
        request.setDocumentNumber("20123456789");

        when(repository.findByDocumentTypeAndDocumentNumber("RUC", "20123456789"))
                .thenReturn(Maybe.empty());
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

        TestObserver<CustomerResponse> observer = service.create(request, "corr-1").test();
        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    void createBusinessCustomerWithDniFailsWithConflict() {
        CustomerCreateRequest request = validRequest();
        request.setCustomerType(CustomerType.BUSINESS);
        request.setDocumentType(DocumentType.DNI);

        TestObserver<CustomerResponse> observer = service.create(request, "corr-1").test();
        observer.assertError(CustomerConflictException.class);
    }

    @Test
    void createPersonalCustomerWithRucFailsWithConflict() {
        CustomerCreateRequest request = validRequest();
        request.setCustomerType(CustomerType.PERSONAL);
        request.setDocumentType(DocumentType.RUC);

        TestObserver<CustomerResponse> observer = service.create(request, "corr-1").test();
        observer.assertError(CustomerConflictException.class);
    }

    @Test
    void findByIdNotFoundFails() {
        when(repository.findById(any(UUID.class))).thenReturn(Maybe.empty());
        TestObserver<CustomerResponse> observer = service.findById(UUID.randomUUID()).test();
        observer.assertError(com.banking.customer.exception.CustomerNotFoundException.class);
    }
}

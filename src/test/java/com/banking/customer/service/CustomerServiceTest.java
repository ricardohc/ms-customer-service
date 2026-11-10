package com.banking.customer.service;

import com.banking.customer.document.CustomerDocument;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerPage;
import com.banking.customer.dto.CustomerProfile;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerStatus;
import com.banking.customer.dto.CustomerType;
import com.banking.customer.dto.CustomerUpdateRequest;
import com.banking.customer.dto.DocumentType;
import com.banking.customer.event.CustomerEventProducer;
import com.banking.customer.event.dto.CustomerEvent;
import com.banking.customer.exception.CustomerConflictException;
import com.banking.customer.exception.CustomerNotFoundException;
import com.banking.customer.mapper.CustomerMapper;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.service.impl.CustomerServiceImpl;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl - logica de negocio")
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

    private CustomerCreateRequest validPersonalRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCustomerType(CustomerType.PERSONAL);
        request.setDocumentType(DocumentType.DNI);
        request.setDocumentNumber("12345678");
        request.setFirstName("Juan");
        request.setLastName("Perez");
        request.setEmail("juan@example.com");
        request.setPhoneNumber("999888777");
        request.setProfile(CustomerProfile.VIP);
        return request;
    }

    private CustomerCreateRequest validBusinessRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCustomerType(CustomerType.BUSINESS);
        request.setDocumentType(DocumentType.RUC);
        request.setDocumentNumber("20123456789");
        request.setFirstName("Acme");
        request.setLastName("Corp");
        request.setEmail("contact@acme.com");
        request.setProfile(CustomerProfile.PYME);
        return request;
    }

    private CustomerDocument existingActiveCustomer(UUID id) {
        return CustomerDocument.builder()
                .id(id)
                .customerType("PERSONAL")
                .documentType("DNI")
                .documentNumber("12345678")
                .firstName("Juan")
                .lastName("Perez")
                .email("juan@example.com")
                .status("ACTIVE")
                .profile("VIP")
                .build();
    }

    private void stubSaveEcho() {
        when(repository.save(any(CustomerDocument.class)))
                .thenAnswer(inv -> Single.just(inv.getArgument(0)));
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("PERSONAL + DNI + VIP crea y publica CustomerCreated con payload completo")
        void personalValidCreatesAndPublishesFullEvent() {
            CustomerCreateRequest request = validPersonalRequest();
            when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                    .thenReturn(Maybe.empty());
            stubSaveEcho();

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertComplete();
            observer.assertNoErrors();

            ArgumentCaptor<CustomerEvent> captor =
                    ArgumentCaptor.forClass(CustomerEvent.class);
            verify(eventProducer).publish(captor.capture());

            CustomerEvent event = captor.getValue();
            assertThat(event.getEventType()).isEqualTo("i");
            assertThat(event.getCorrelationId()).isEqualTo("corr-1");
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getOccurredAt()).isNotNull();

            CustomerEvent.CustomerPayload payload = event.getPayload();
            assertThat(payload.getCustomerId()).isNotNull();
            assertThat(payload.getCustomerType()).isEqualTo("PERSONAL");
            assertThat(payload.getDocumentType()).isEqualTo("DNI");
            assertThat(payload.getDocumentNumber()).isEqualTo("12345678");
            assertThat(payload.getFirstName()).isEqualTo("Juan");
            assertThat(payload.getLastName()).isEqualTo("Perez");
            assertThat(payload.getEmail()).isEqualTo("juan@example.com");
            assertThat(payload.getPhoneNumber()).isEqualTo("999888777");
            assertThat(payload.getProfile()).isEqualTo("VIP");
            assertThat(payload.getStatus()).isEqualTo("ACTIVE");
            assertThat(payload.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("BUSINESS + RUC + PYME crea y publica CustomerCreated")
        void businessValidCreates() {
            CustomerCreateRequest request = validBusinessRequest();
            when(repository.findByDocumentTypeAndDocumentNumber("RUC", "20123456789"))
                    .thenReturn(Maybe.empty());
            stubSaveEcho();

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertComplete();
            observer.assertNoErrors();
            verify(eventProducer).publish(any(CustomerEvent.class));
        }

        @Test
        @DisplayName("Documento duplicado produce CustomerConflictException y no publica")
        void duplicateDocumentConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            CustomerDocument existing = CustomerDocument.builder()
                    .id(UUID.randomUUID())
                    .documentType("DNI")
                    .documentNumber("12345678")
                    .build();
            when(repository.findByDocumentTypeAndDocumentNumber("DNI", "12345678"))
                    .thenReturn(Maybe.just(existing));

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("customerType nulo produce CustomerConflictException")
        void nullCustomerTypeConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            request.setCustomerType(null);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("documentType nulo produce CustomerConflictException")
        void nullDocumentTypeConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            request.setDocumentType(null);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("BUSINESS con DNI produce CustomerConflictException")
        void businessWithDniConflicts() {
            CustomerCreateRequest request = validBusinessRequest();
            request.setDocumentType(DocumentType.DNI);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("PERSONAL con RUC produce CustomerConflictException")
        void personalWithRucConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            request.setDocumentType(DocumentType.RUC);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("PERSONAL con profile PYME produce CustomerConflictException")
        void personalWithBusinessProfileConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            request.setProfile(CustomerProfile.PYME);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("BUSINESS con profile VIP produce CustomerConflictException")
        void businessWithPersonalProfileConflicts() {
            CustomerCreateRequest request = validBusinessRequest();
            request.setProfile(CustomerProfile.VIP);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("profile nulo produce CustomerConflictException")
        void nullProfileConflicts() {
            CustomerCreateRequest request = validPersonalRequest();
            request.setProfile(null);

            TestObserver<CustomerResponse> observer =
                    service.create(request, "corr-1").test();

            observer.assertError(CustomerConflictException.class);
            verify(eventProducer, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Cliente existente devuelve CustomerResponse mapeado")
        void existsReturnsResponse() {
            UUID id = UUID.randomUUID();
            CustomerDocument existing = existingActiveCustomer(id);
            when(repository.findById(id)).thenReturn(Maybe.just(existing));

            TestObserver<CustomerResponse> observer =
                    service.findById(id).test();

            observer.assertComplete();
            observer.assertNoErrors();
            observer.assertValue(response -> {
                assertThat(response.getId()).isEqualTo(id);
                assertThat(response.getCustomerType()).isEqualTo(CustomerType.PERSONAL);
                assertThat(response.getDocumentType()).isEqualTo(DocumentType.DNI);
                assertThat(response.getProfile()).isEqualTo(CustomerProfile.VIP);
                assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
                return true;
            });
        }

        @Test
        @DisplayName("Cliente inexistente produce CustomerNotFoundException")
        void notFoundFails() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Maybe.empty());

            TestObserver<CustomerResponse> observer =
                    service.findById(id).test();

            observer.assertError(CustomerNotFoundException.class);
            observer.assertValueCount(0);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Devuelve CustomerPage con content y totalElements")
        void returnsPageWithContentAndTotal() {
            CustomerDocument a = existingActiveCustomer(UUID.randomUUID());
            CustomerDocument b = existingActiveCustomer(UUID.randomUUID());
            when(repository.findAllBy(any())).thenReturn(Flowable.just(a, b));
            when(repository.countBy()).thenReturn(Single.just(2L));

            TestObserver<CustomerPage> observer =
                    service.findAll(0, 10).test();

            observer.assertComplete();
            observer.assertNoErrors();
            observer.assertValue(page -> {
                assertThat(page.getContent()).hasSize(2);
                assertThat(page.getPage()).isEqualTo(0);
                assertThat(page.getSize()).isEqualTo(10);
                assertThat(page.getTotalElements()).isEqualTo(2);
                return true;
            });
        }

        @Test
        @DisplayName("Lista vacia devuelve page vacia y total 0")
        void emptyPage() {
            when(repository.findAllBy(any())).thenReturn(Flowable.empty());
            when(repository.countBy()).thenReturn(Single.just(0L));

            TestObserver<CustomerPage> observer =
                    service.findAll(0, 10).test();

            observer.assertComplete();
            observer.assertValue(page -> {
                assertThat(page.getContent()).isEmpty();
                assertThat(page.getTotalElements()).isZero();
                return true;
            });
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Cambio de email actualiza y publica CustomerUpdated con payload completo")
        void updatesEmailAndPublishesEvent() {
            UUID id = UUID.randomUUID();
            CustomerDocument existing = existingActiveCustomer(id);
            CustomerUpdateRequest update = new CustomerUpdateRequest();
            update.setEmail("nuevo@example.com");

            when(repository.findById(id)).thenReturn(Maybe.just(existing));
            stubSaveEcho();

            TestObserver<CustomerResponse> observer =
                    service.update(id, update, "corr-2").test();

            observer.assertComplete();
            observer.assertNoErrors();

            ArgumentCaptor<CustomerEvent> captor =
                    ArgumentCaptor.forClass(CustomerEvent.class);
            verify(eventProducer).publish(captor.capture());

            CustomerEvent event = captor.getValue();
            assertThat(event.getEventType()).isEqualTo("u");
            assertThat(event.getCorrelationId()).isEqualTo("corr-2");
            assertThat(event.getPayload().getCustomerId()).isEqualTo(id);
            assertThat(event.getPayload().getEmail()).isEqualTo("nuevo@example.com");
            assertThat(event.getPayload().getProfile()).isEqualTo("VIP");
            assertThat(event.getPayload().getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Cambiar customerType produce CustomerConflictException y no actualiza")
        void changingCustomerTypeConflicts() {
            UUID id = UUID.randomUUID();
            CustomerUpdateRequest update = new CustomerUpdateRequest();
            update.setCustomerType(CustomerType.BUSINESS);

            TestObserver<CustomerResponse> observer =
                    service.update(id, update, "corr-2").test();

            observer.assertError(CustomerConflictException.class);
            verify(repository, never()).findById(any(UUID.class));
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("Cliente inexistente produce CustomerNotFoundException")
        void updateNotFoundFails() {
            UUID id = UUID.randomUUID();
            CustomerUpdateRequest update = new CustomerUpdateRequest();
            update.setEmail("nuevo@example.com");
            when(repository.findById(id)).thenReturn(Maybe.empty());

            TestObserver<CustomerResponse> observer =
                    service.update(id, update, "corr-2").test();

            observer.assertError(CustomerNotFoundException.class);
            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class Deactivate {

        @Test
        @DisplayName("Cliente activo pasa a INACTIVE y publica CustomerDeactivated")
        void activeBecomesInactiveAndPublishes() {
            UUID id = UUID.randomUUID();
            CustomerDocument existing = existingActiveCustomer(id);
            when(repository.findById(id)).thenReturn(Maybe.just(existing));
            stubSaveEcho();

            service.deactivate(id, "corr-3").test()
                    .assertComplete()
                    .assertNoErrors();

            ArgumentCaptor<CustomerEvent> captor =
                    ArgumentCaptor.forClass(CustomerEvent.class);
            verify(eventProducer).publish(captor.capture());

            CustomerEvent event = captor.getValue();
            assertThat(event.getEventType()).isEqualTo("d");
            assertThat(event.getCorrelationId()).isEqualTo("corr-3");
            assertThat(event.getPayload().getStatus()).isEqualTo("INACTIVE");
            assertThat(event.getPayload().getCustomerId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Cliente ya inactivo produce CustomerConflictException y no publica")
        void alreadyInactiveConflicts() {
            UUID id = UUID.randomUUID();
            CustomerDocument existing = existingActiveCustomer(id);
            existing.setStatus("INACTIVE");
            when(repository.findById(id)).thenReturn(Maybe.just(existing));

            service.deactivate(id, "corr-3").test()
                    .assertError(CustomerConflictException.class);

            verify(repository, never()).save(any());
            verify(eventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("Cliente inexistente produce CustomerNotFoundException")
        void deactivateNotFoundFails() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Maybe.empty());

            service.deactivate(id, "corr-3").test()
                    .assertError(CustomerNotFoundException.class);

            verify(eventProducer, never()).publish(any());
        }
    }
}
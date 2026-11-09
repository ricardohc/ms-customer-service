package com.banking.customer.event;

import com.banking.customer.document.CustomerDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio en el tópico <code>banking.customer.events.v1</code>.
 * Envelope obligatorio: eventId, eventType, occurredAt, correlationId, payload.
 * (Payloads a formalizar en el catálogo Kafka oficial antes de Parte III.)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEvent {

    public enum Type {
        CustomerCreated, CustomerUpdated, CustomerDeactivated
    }

    private UUID eventId;
    private Type eventType;
    private Instant occurredAt;
    private String correlationId;
    private CustomerPayload payload;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPayload {
        private UUID customerId;
        private String customerType;
        private String documentType;
        private String documentNumber;
        private String email;
        private String profile;
        private String status;

        public static CustomerPayload from(CustomerDocument document) {
            return CustomerPayload.builder()
                    .customerId(document.getId())
                    .customerType(document.getCustomerType())
                    .documentType(document.getDocumentType())
                    .documentNumber(document.getDocumentNumber())
                    .email(document.getEmail())
                    .profile(document.getProfile())
                    .status(document.getStatus())
                    .build();
        }
    }

    public static CustomerEvent of(Type type, CustomerDocument document, String correlationId) {
        return CustomerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(type)
                .occurredAt(Instant.now())
                .correlationId(correlationId)
                .payload(CustomerPayload.from(document))
                .build();
    }
}

package com.banking.customer.event.dto;

import com.banking.customer.document.CustomerDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEvent {

    public enum Type {
        CustomerCreated("i"),
        CustomerUpdated("u"),
        CustomerDeactivated("d");

        private final String code;

        Type(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private UUID eventId;
    private String eventType;
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
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String profile;
        private String status;
        private Instant createdAt;

        public static CustomerPayload from(CustomerDocument document) {
            return CustomerPayload.builder()
                    .customerId(document.getId())
                    .customerType(document.getCustomerType())
                    .documentType(document.getDocumentType())
                    .documentNumber(document.getDocumentNumber())
                    .firstName(document.getFirstName())
                    .lastName(document.getLastName())
                    .email(document.getEmail())
                    .phoneNumber(document.getPhoneNumber())
                    .profile(document.getProfile())
                    .status(document.getStatus())
                    .createdAt(document.getCreatedAt())
                    .build();
        }
    }

    public static CustomerEvent of(Type type, CustomerDocument document, String correlationId) {
        return CustomerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(type.getCode())
                .occurredAt(Instant.now())
                .correlationId(correlationId)
                .payload(CustomerPayload.from(document))
                .build();
    }
}
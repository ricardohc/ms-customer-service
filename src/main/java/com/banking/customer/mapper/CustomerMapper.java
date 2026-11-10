package com.banking.customer.mapper;

import com.banking.customer.document.CustomerDocument;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerMapper {

    public CustomerDocument toDocument(CustomerCreateRequest request) {
        return CustomerDocument.builder()
                .customerType(request.getCustomerType() != null ? request.getCustomerType().getValue() : null)
                .documentType(request.getDocumentType() != null ? request.getDocumentType().getValue() : null)
                .documentNumber(request.getDocumentNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .profile(request.getProfile() != null ? request.getProfile().getValue() : null)
                .build();
    }

    public CustomerResponse toResponse(CustomerDocument document) {
        CustomerResponse response = new CustomerResponse();
        response.setId(document.getId() != null ? document.getId() : UUID.randomUUID());
        if (document.getCustomerType() != null) {
            response.setCustomerType(com.banking.customer.dto.CustomerType.fromValue(document.getCustomerType()));
        }
        if (document.getDocumentType() != null) {
            response.setDocumentType(com.banking.customer.dto.DocumentType.fromValue(document.getDocumentType()));
        }
        response.setDocumentNumber(document.getDocumentNumber());
        response.setFirstName(document.getFirstName());
        response.setLastName(document.getLastName());
        response.setEmail(document.getEmail());
        response.setPhoneNumber(document.getPhoneNumber());
        if (document.getProfile() != null) {
            response.setProfile(com.banking.customer.dto.CustomerProfile.fromValue(document.getProfile()));
        }
        if (document.getStatus() != null) {
            response.setStatus(com.banking.customer.dto.CustomerStatus.fromValue(document.getStatus()));
        }
        if (document.getCreatedAt() != null) {
            response.setCreatedAt(java.time.OffsetDateTime.ofInstant(document.getCreatedAt(), java.time.ZoneOffset.UTC));
        }
        return response;
    }

    public void applyUpdate(CustomerDocument document, CustomerUpdateRequest request) {
        if (request.getFirstName() != null) {
            document.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            document.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            document.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            document.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getProfile() != null) {
            document.setProfile(request.getProfile().getValue());
        }
        if (request.getStatus() != null) {
            document.setStatus(request.getStatus().getValue());
        }
    }
}
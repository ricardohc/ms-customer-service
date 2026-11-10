package com.banking.customer.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
@CompoundIndex(name = "uk_document", def = "{'documentType': 1, 'documentNumber': 1}", unique = true)
public class CustomerDocument {

    @Id
    private UUID id;

    @Field("customerType")
    private String customerType;

    @Field("documentType")
    private String documentType;

    @Field("documentNumber")
    private String documentNumber;

    @Field("firstName")
    private String firstName;

    @Field("lastName")
    private String lastName;

    @Field("email")
    private String email;

    @Field("phoneNumber")
    private String phoneNumber;

    @Field("profile")
    private String profile;

    @Field("status")
    private String status;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @Version
    private Long version;
}
package com.banking.customer.controller;

import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.dto.CustomerResponse;
import com.banking.customer.dto.CustomerType;
import com.banking.customer.dto.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

/**
 * Prueba de integración de extremo a extremo del contrato HTTP.
 * Usa MongoDB embebido (Flapdoodle) según docs/common-rules.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CustomerApiIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/banking_customer_test");
        registry.add("spring.security.dev-mode", () -> "true");
    }

    private CustomerCreateRequest validRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setCustomerType(CustomerType.BUSINESS);
        request.setDocumentType(DocumentType.RUC);
        request.setDocumentNumber("20" + UUID.randomUUID().toString().substring(0, 9));
        request.setFirstName("Acme");
        request.setLastName("Corp");
        request.setEmail("acme@example.com");
        return request;
    }

    @Test
    void createCustomerReturns201AndBody() {
        webTestClient.post()
                .uri("/api/v1/customers")
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("X-Channel-ID", "WEB")
                .header("X-Transaction-ID", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void createDuplicateDocumentReturns409() {
        CustomerCreateRequest request = validRequest();
        String correlation = UUID.randomUUID().toString();
        String tx = UUID.randomUUID().toString();
        webTestClient.post()
                .uri("/api/v1/customers")
                .header("X-Correlation-ID", correlation)
                .header("X-Channel-ID", "WEB")
                .header("X-Transaction-ID", tx)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/customers")
                .header("X-Correlation-ID", correlation)
                .header("X-Channel-ID", "WEB")
                .header("X-Transaction-ID", tx)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getUnknownCustomerReturns404() {
        webTestClient.get()
                .uri("/api/v1/customers/{id}", UUID.randomUUID())
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("X-Channel-ID", "WEB")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteCustomerReturns204() {
        CustomerCreateRequest request = validRequest();
        CustomerResponse created = webTestClient.post()
                .uri("/api/v1/customers")
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("X-Channel-ID", "WEB")
                .header("X-Transaction-ID", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponse.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete()
                .uri("/api/v1/customers/{id}", created.getId())
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("X-Channel-ID", "WEB")
                .header("X-Transaction-ID", UUID.randomUUID().toString())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void listCustomersReturns200() {
        webTestClient.get()
                .uri("/api/v1/customers?page=0&size=10")
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("X-Channel-ID", "WEB")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10);
    }
}

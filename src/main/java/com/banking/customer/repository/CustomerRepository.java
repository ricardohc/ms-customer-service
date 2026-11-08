package com.banking.customer.repository;

import com.banking.customer.document.CustomerDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Acceso reactivo a la colección <code>customers</code> (base <code>banking_customer</code>).
 * Solo métodos derivados de Spring Data; sin @Query ni SQL dinámico.
 */
@Repository
public interface CustomerRepository extends ReactiveCrudRepository<CustomerDocument, UUID> {

    Mono<CustomerDocument> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Flux<CustomerDocument> findAllBy(Pageable pageable);

    Mono<Long> countBy();
}

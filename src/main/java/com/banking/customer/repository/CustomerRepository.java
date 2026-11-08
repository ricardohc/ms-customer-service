package com.banking.customer.repository;

import com.banking.customer.document.CustomerDocument;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepository extends RxJava3CrudRepository<CustomerDocument, UUID> {

    Maybe<CustomerDocument> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Flowable<CustomerDocument> findAllBy(Pageable pageable);

    Single<Long> countBy();
}
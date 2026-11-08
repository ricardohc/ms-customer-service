package com.banking.customer.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Productor de eventos de dominio hacia el tópico banking.customer.events.v1.
 * Clave del mensaje: customerId (para orden por cliente). Envío asíncrono sin
 * bloquear el camino reactivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventProducer {

    public static final String TOPIC = "banking.customer.events.v1";

    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    public void publishCreated(com.banking.customer.document.CustomerDocument document, String correlationId) {
        publish(CustomerEvent.of(CustomerEvent.Type.CustomerCreated, document, correlationId), document.getId());
    }

    public void publishUpdated(com.banking.customer.document.CustomerDocument document, String correlationId) {
        publish(CustomerEvent.of(CustomerEvent.Type.CustomerUpdated, document, correlationId), document.getId());
    }

    public void publishDeactivated(com.banking.customer.document.CustomerDocument document, String correlationId) {
        publish(CustomerEvent.of(CustomerEvent.Type.CustomerDeactivated, document, correlationId), document.getId());
    }

    private void publish(CustomerEvent event, java.util.UUID key) {
        // Kafka send() puede bloquear esperando metadata; lo aislamos del camino reactivo.
        Mono.fromRunnable(() -> kafkaTemplate.send(TOPIC, key.toString(), event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event {} for customer {}", event.getEventType(), key, ex);
                            } else {
                                log.debug("Event {} published for customer {}", event.getEventType(), key);
                            }
                        }))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}

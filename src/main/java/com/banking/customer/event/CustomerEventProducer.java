package com.banking.customer.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Productor de eventos de dominio hacia el tópico banking.customer.events.v1.
 *
 * Clave del mensaje: customerId (garantiza orden por cliente).
 *
 * Notas:
 * - kafkaTemplate.send() es no bloqueante por naturaleza: devuelve un CompletableFuture.
 *   NO se debe envolver en Mono.fromRunnable(...).subscribe() porque el Runnable
 *   termina inmediatamente sin esperar el futuro, y los errores se pierden.
 * - Si el broker no está disponible o el tópico no existe (con auto.create deshabilitado),
 *   el error se registra aquí vía whenComplete.
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

    private void publish(CustomerEvent event, UUID key) {
        CompletableFuture<SendResult<String, CustomerEvent>> future =
                kafkaTemplate.send(TOPIC, key.toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} for customer {} to topic {}: {}",
                        event.getEventType(), key, TOPIC, ex.getMessage(), ex);
            } else if (log.isDebugEnabled()) {
                log.debug("Event {} published for customer {} to topic {} partition {} offset {}",
                        event.getEventType(), key, TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
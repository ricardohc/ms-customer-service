package com.banking.customer.event.impl;

import com.banking.customer.event.CustomerEventProducer;
import com.banking.customer.event.dto.CustomerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventProducerImpl implements CustomerEventProducer {

    public static final String TOPIC = "banking.customer.events.v1";

    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    public void publish(CustomerEvent event) {
        String key = event.getPayload().getCustomerId().toString();
        CompletableFuture<SendResult<String, CustomerEvent>> future =
                kafkaTemplate.send(TOPIC, key, event);

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
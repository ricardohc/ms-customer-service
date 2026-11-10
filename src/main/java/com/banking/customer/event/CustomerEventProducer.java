package com.banking.customer.event;

import com.banking.customer.event.dto.CustomerEvent;

public interface CustomerEventProducer {

    void publish(CustomerEvent event);
}
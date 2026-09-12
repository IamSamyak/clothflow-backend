package com.clothflow.payment.outbox;

import com.clothflow.payment.entity.OutboxEvent;

public interface OutboxEventPublisher {

    void publish(OutboxEvent event);
}
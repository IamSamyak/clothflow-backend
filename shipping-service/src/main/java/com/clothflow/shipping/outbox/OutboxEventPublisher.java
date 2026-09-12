package com.clothflow.shipping.outbox;

import com.clothflow.shipping.entity.OutboxEvent;

public interface OutboxEventPublisher {

    void publish(OutboxEvent event);
}
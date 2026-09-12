package com.clothflow.shipping.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope<T>(

        UUID eventId,

        String eventType,

        String aggregateType,

        UUID aggregateId,

        OffsetDateTime occurredAt,

        T payload
) {
}
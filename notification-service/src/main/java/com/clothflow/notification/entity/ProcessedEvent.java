package com.clothflow.notification.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "processed_event",
        indexes = {
                @Index(
                        name = "idx_processed_event_processed_at",
                        columnList = "processed_at"
                ),
                @Index(
                        name = "idx_processed_event_aggregate",
                        columnList = "aggregate_type, aggregate_id"
                )
        }
)
public class ProcessedEvent {

    @Id
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false
    )
    private UUID eventId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100,
            updatable = false
    )
    private String eventType;

    @Column(
            name = "aggregate_type",
            length = 100,
            updatable = false
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            updatable = false
    )
    private UUID aggregateId;

    @Column(
            name = "processed_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.processedAt =
                OffsetDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
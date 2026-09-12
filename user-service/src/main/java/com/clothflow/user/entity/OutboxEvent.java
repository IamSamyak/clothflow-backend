package com.clothflow.user.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(
            name = "aggregate_type",
            nullable = false,
            length = 100
    )
    private String aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false
    )
    private UUID aggregateId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private OutboxEventStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;


    @Column(
            name = "retry_count",
            nullable = false
    )
    private int retryCount;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload
    ) {

        if (aggregateType == null ||
                aggregateType.isBlank()) {

            throw new IllegalArgumentException(
                    "Aggregate type must not be blank"
            );
        }

        if (aggregateId == null) {

            throw new IllegalArgumentException(
                    "Aggregate ID must not be null"
            );
        }

        if (eventType == null ||
                eventType.isBlank()) {

            throw new IllegalArgumentException(
                    "Event type must not be blank"
            );
        }

        if (payload == null ||
                payload.isBlank()) {

            throw new IllegalArgumentException(
                    "Payload must not be blank"
            );
        }

        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.retryCount = 0;
        this.nextAttemptAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public OffsetDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markProcessing() {

        if (status != OutboxEventStatus.PENDING) {

            throw new IllegalStateException(
                    "Only pending events can be processed"
            );
        }

        this.status =
                OutboxEventStatus.PROCESSING;

        this.processingStartedAt =
                OffsetDateTime.now();
    }

    public void markPublished() {

        if (status != OutboxEventStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Only processing events can be published"
            );
        }

        this.status =
                OutboxEventStatus.PUBLISHED;

        this.publishedAt =
                OffsetDateTime.now();

        this.processingStartedAt = null;

        this.lastError = null;
    }

    public void markRetry(
            String error,
            OffsetDateTime nextAttemptAt
    ) {

        if (status != OutboxEventStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Only processing events can be retried"
            );
        }

        this.retryCount++;

        this.status =
                OutboxEventStatus.PENDING;

        this.processingStartedAt = null;

        this.lastError = error;

        this.nextAttemptAt =
                nextAttemptAt;
    }

    public void markFailed(
            String error
    ) {

        if (status != OutboxEventStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Only processing events can fail"
            );
        }

        this.retryCount++;

        this.status =
                OutboxEventStatus.FAILED;

        this.processingStartedAt = null;

        this.lastError = error;
    }

    public void setProcessingStartedAtForTest(
                  OffsetDateTime timestamp
         ) {
            this.processingStartedAt = timestamp;
          }
}
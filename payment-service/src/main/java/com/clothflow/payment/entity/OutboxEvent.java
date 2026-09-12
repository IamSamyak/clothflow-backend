package com.clothflow.payment.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "outbox_event")
@Setter
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String payload
    ) {
        this.id = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = OffsetDateTime.now();
        this.nextAttemptAt = this.createdAt;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markProcessing(OffsetDateTime leaseUntil) {
        this.status = OutboxEventStatus.PROCESSING;
        this.nextAttemptAt = leaseUntil;
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.processedAt = OffsetDateTime.now();
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markForRetry(
            OffsetDateTime nextAttemptAt,
            String error
    ) {
        this.status = OutboxEventStatus.PENDING;
        this.retryCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error;
    }

    public void recoverFromStaleProcessing() {
        this.status = OutboxEventStatus.PENDING;
        this.nextAttemptAt = OffsetDateTime.now();
    }
}
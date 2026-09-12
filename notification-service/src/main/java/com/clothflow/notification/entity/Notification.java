package com.clothflow.notification.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notifications_customer_id",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_notifications_status_attempt",
                        columnList = "status, next_attempt_at"
                ),
                @Index(
                        name = "idx_notifications_event_type",
                        columnList = "event_type"
                ),
                @Index(
                        name = "idx_notifications_created_at",
                        columnList = "created_at"
                )
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "customer_id",
            nullable = false,
            updatable = false
    )
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private NotificationChannel channel;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100,
            updatable = false
    )
    private String eventType;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private NotificationStatus status;

    @Column(
            name = "retry_count",
            nullable = false
    )
    private int retryCount;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "notification",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<NotificationDeliveryAttempt>
            deliveryAttempts = new ArrayList<>();

    protected Notification() {
    }

    public Notification(
            UUID customerId,
            NotificationChannel channel,
            String eventType,
            String subject,
            String content
    ) {
//        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.channel = channel;
        this.eventType = eventType;
        this.subject = subject;
        this.content = content;

        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;

        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;

        /*
         * Make the notification immediately eligible
         * for the delivery scheduler.
         */
        this.nextAttemptAt = this.createdAt;

        this.deliveryAttempts =
                new ArrayList<>();
    }

    public void addDeliveryAttempt(
            NotificationDeliveryAttempt attempt
    ) {
        deliveryAttempts.add(attempt);
        attempt.setNotification(this);
    }

    public void markProcessing(
            OffsetDateTime leaseUntil
    ) {
        this.status =
                NotificationStatus.PROCESSING;

        this.nextAttemptAt =
                leaseUntil;

        this.updatedAt =
                OffsetDateTime.now();
    }

    public void markSent() {

        this.status =
                NotificationStatus.SENT;

        this.sentAt =
                OffsetDateTime.now();

        this.nextAttemptAt =
                null;

        this.lastError =
                null;

        this.updatedAt =
                OffsetDateTime.now();
    }

    public void markForRetry(
            OffsetDateTime nextAttemptAt,
            String error
    ) {

        this.status =
                NotificationStatus.PENDING;

        this.retryCount++;

        this.nextAttemptAt =
                nextAttemptAt;

        this.lastError =
                error;

        this.updatedAt =
                OffsetDateTime.now();
    }

    public void markFailed(
            String error
    ) {

        this.status =
                NotificationStatus.FAILED;

        this.lastError =
                error;

        this.nextAttemptAt =
                null;

        this.updatedAt =
                OffsetDateTime.now();
    }

    public void recoverFromStaleProcessing() {

        this.status =
                NotificationStatus.PENDING;

        this.nextAttemptAt =
                OffsetDateTime.now();

        this.updatedAt =
                OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt =
                OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public NotificationStatus getStatus() {
        return status;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<NotificationDeliveryAttempt>
    getDeliveryAttempts() {
        return deliveryAttempts;
    }
}
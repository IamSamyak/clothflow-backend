package com.clothflow.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
        name = "notification_delivery_attempt",
        indexes = {
                @Index(
                        name = "idx_delivery_attempt_notification",
                        columnList = "notification_id"
                )
        }
)
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "notification_id",
            nullable = false
    )
    private Notification notification;

    @Column(
            name = "attempt_number",
            nullable = false
    )
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private DeliveryAttemptStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(
            name = "attempted_at",
            nullable = false
    )
    private OffsetDateTime attemptedAt;

    protected NotificationDeliveryAttempt() {
    }

    public NotificationDeliveryAttempt(
            int attemptNumber,
            DeliveryAttemptStatus status,
            String errorMessage
    ) {
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.errorMessage = errorMessage;
        this.attemptedAt =
                OffsetDateTime.now();
    }

    public void markSent() {

        this.status =
                DeliveryAttemptStatus.SENT;

        this.attemptedAt =
                OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {

        this.status =
                DeliveryAttemptStatus.FAILED;

        this.errorMessage =
                errorMessage;

        this.attemptedAt =
                OffsetDateTime.now();
    }

    public void markAbandoned(
            String reason
    ) {

        this.status =
                DeliveryAttemptStatus.ABANDONED;

        this.errorMessage =
                reason;
    }
}
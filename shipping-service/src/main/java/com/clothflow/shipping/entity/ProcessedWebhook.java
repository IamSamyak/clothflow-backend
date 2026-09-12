package com.clothflow.shipping.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "processed_webhook",
        indexes = {
                @Index(
                        name = "idx_processed_webhook_processed_at",
                        columnList = "processed_at"
                ),
                @Index(
                        name = "idx_processed_webhook_tracking_number",
                        columnList = "tracking_number"
                )
        }
)
public class ProcessedWebhook {

    @Id
    @Column(
            name = "provider_event_id",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String providerEventId;

    @Column(
            nullable = false,
            length = 100,
            updatable = false
    )
    private String provider;

    @Column(
            name = "event_type",
            length = 100,
            updatable = false
    )
    private String eventType;

    @Column(
            name = "tracking_number",
            length = 100,
            updatable = false
    )
    private String trackingNumber;

    @Column(
            name = "processed_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime processedAt;

    protected ProcessedWebhook() {
    }

    public ProcessedWebhook(
            String providerEventId,
            String provider,
            String eventType,
            String trackingNumber
    ) {
        this.providerEventId =
                providerEventId;

        this.provider =
                provider;

        this.eventType =
                eventType;

        this.trackingNumber =
                trackingNumber;

        this.processedAt =
                OffsetDateTime.now();
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public String getProvider() {
        return provider;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
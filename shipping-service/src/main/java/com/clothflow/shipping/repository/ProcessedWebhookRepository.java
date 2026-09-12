package com.clothflow.shipping.repository;

import com.clothflow.shipping.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedWebhookRepository
        extends JpaRepository<
        ProcessedWebhook,
        String
        > {

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_webhook (
                        provider_event_id,
                        provider,
                        event_type,
                        tracking_number,
                        processed_at
                    )
                    VALUES (
                        :providerEventId,
                        :provider,
                        :eventType,
                        :trackingNumber,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (provider_event_id)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfNotProcessed(
            @Param("providerEventId")
            String providerEventId,

            @Param("provider")
            String provider,

            @Param("eventType")
            String eventType,

            @Param("trackingNumber")
            String trackingNumber
    );
}
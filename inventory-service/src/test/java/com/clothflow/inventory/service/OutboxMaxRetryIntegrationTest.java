package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMaxRetryIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxStatusService outboxStatusService;

    @Test
    void shouldMarkEventAsFailedAfterMaximumRetries() {

        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("InventoryReservation");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("INVENTORY_RESERVED");

        event.setPayload("""
                {
                  "reservationId": "test"
                }
                """);

        event.setStatus(
                OutboxEventStatus.PROCESSING
        );

        event.setProcessingStartedAt(
                LocalDateTime.now()
        );

        // Four retries have already happened.
        event.setRetryCount(4);

        OutboxEvent saved =
                outboxEventRepository
                        .saveAndFlush(event);

        outboxStatusService.markFailed(
                saved,
                "Kafka broker unavailable"
        );

        OutboxEvent failedEvent =
                outboxEventRepository
                        .findById(saved.getId())
                        .orElseThrow();

        assertThat(failedEvent.getRetryCount())
                .isEqualTo(5);

        assertThat(failedEvent.getStatus())
                .isEqualTo(OutboxEventStatus.FAILED);

        assertThat(failedEvent.getLastError())
                .isEqualTo("Kafka broker unavailable");

        assertThat(failedEvent.getProcessingStartedAt())
                .isNull();

        assertThat(failedEvent.getNextAttemptAt())
                .isNull();
    }
}
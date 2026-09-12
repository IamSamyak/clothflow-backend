package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxFailureRecoveryIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxStatusService outboxStatusService;

    @Autowired
    private OutboxRetryPolicy retryPolicy;

    @Test
    void shouldRetryFailedOutboxEvent() {

        OutboxEvent event = new OutboxEvent();

        event.setAggregateType("InventoryReservation");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("INVENTORY_RESERVED");
        event.setPayload("""
            {
              "reservationId": "test"
            }
            """);

        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setProcessingStartedAt(LocalDateTime.now());
        event.setRetryCount(0);

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        outboxStatusService.markFailed(
                saved,
                "Kafka broker unavailable"
        );

        OutboxEvent failedEvent =
                outboxEventRepository
                        .findById(saved.getId())
                        .orElseThrow();

        assertThat(failedEvent.getRetryCount())
                .isEqualTo(1);

        assertThat(failedEvent.getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(failedEvent.getLastError())
                .isEqualTo("Kafka broker unavailable");

        assertThat(failedEvent.getProcessingStartedAt())
                .isNull();

        assertThat(failedEvent.getNextAttemptAt())
                .isNotNull();

        LocalDateTime now = LocalDateTime.now();

        assertThat(failedEvent.getNextAttemptAt())
                .isBetween(
                        now.minusSeconds(1),
                        now.plusSeconds(5)
                );
    }
}
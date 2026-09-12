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

class OutboxStaleRecoveryIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxClaimService outboxClaimService;

    @Test
    void shouldRecoverStaleProcessingEvent() {

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

        event.setProcessingStartedAt(
                LocalDateTime.now().minusMinutes(5)
        );

        event.setRetryCount(1);

        OutboxEvent saved =
                outboxEventRepository.saveAndFlush(event);

        LocalDateTime cutoff =
                LocalDateTime.now().minusMinutes(2);

        int recovered =
                outboxClaimService.recoverStaleEvents(cutoff);

        assertThat(recovered)
                .isEqualTo(1);

        OutboxEvent recoveredEvent =
                outboxEventRepository
                        .findById(saved.getId())
                        .orElseThrow();

        assertThat(recoveredEvent.getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(recoveredEvent.getProcessingStartedAt())
                .isNull();

        assertThat(recoveredEvent.getNextAttemptAt())
                .isNotNull();
    }
}
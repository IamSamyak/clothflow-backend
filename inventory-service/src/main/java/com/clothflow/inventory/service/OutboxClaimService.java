package com.clothflow.inventory.service;

import com.clothflow.inventory.entity.OutboxEvent;
import com.clothflow.inventory.entity.OutboxEventStatus;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxClaimService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxClaimService(
            OutboxEventRepository outboxEventRepository) {

        this.outboxEventRepository =
                outboxEventRepository;
    }

    /**
     * Claims pending outbox events.
     *
     * Database flow:
     *
     * PENDING
     *    ↓
     * SELECT ... FOR UPDATE SKIP LOCKED
     *    ↓
     * PROCESSING
     *    ↓
     * COMMIT
     *
     * Multiple publisher instances can safely
     * claim different events concurrently.
     */
    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .claimPendingEvents();

        LocalDateTime now =
                LocalDateTime.now();

        for (OutboxEvent event : events) {

            event.setStatus(
                    OutboxEventStatus.PROCESSING
            );

            event.setProcessingStartedAt(now);

            outboxEventRepository.save(event);
        }

        return events;
    }

    /**
     * Recovers events that have been stuck
     * in PROCESSING state.
     *
     * Example:
     *
     * Publisher claims event
     *       ↓
     * PROCESSING
     *       ↓
     * Publisher crashes
     *       ↓
     * Event remains PROCESSING
     *       ↓
     * Recovery detects stale event
     *       ↓
     * PENDING
     */
    @Transactional
    public int recoverStaleEvents(
            LocalDateTime cutoff) {

        List<OutboxEvent> staleEvents =
                outboxEventRepository
                        .findStaleProcessingEvents(cutoff);

        for (OutboxEvent event : staleEvents) {

            event.setStatus(
                    OutboxEventStatus.PENDING
            );

            event.setProcessingStartedAt(
                    null
            );

            event.setNextAttemptAt(
                    LocalDateTime.now()
            );

            outboxEventRepository.save(event);
        }

        return staleEvents.size();
    }
}
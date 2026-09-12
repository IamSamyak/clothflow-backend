package com.clothflow.payment.outbox;

import com.clothflow.payment.entity.OutboxEvent;
import com.clothflow.payment.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxEventClaimService {

    private static final int LEASE_SECONDS = 60;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventClaimService(
            OutboxEventRepository outboxEventRepository
    ) {
        this.outboxEventRepository =
                outboxEventRepository;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        OffsetDateTime now =
                OffsetDateTime.now();

        List<OutboxEvent> events =
                outboxEventRepository
                        .findPendingEventsForUpdate(
                                now
                        );

        OffsetDateTime leaseUntil =
                now.plusSeconds(
                        LEASE_SECONDS
                );

        for (OutboxEvent event : events) {

            event.markProcessing(
                    leaseUntil
            );
        }

        outboxEventRepository.saveAll(events);

        return events;
    }
}
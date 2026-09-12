package com.clothflow.user.service;

import com.clothflow.user.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OutboxRetentionService {

    private final OutboxEventRepository outboxEventRepository;

    private final long retentionDays;

    public OutboxRetentionService(
            OutboxEventRepository outboxEventRepository,
            @Value("${outbox.retention.password-reset-days:1}")
            long retentionDays
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.retentionDays =
                retentionDays;
    }

    @Transactional
    public int cleanupPasswordResetEvents() {

        OffsetDateTime cutoff =
                OffsetDateTime.now()
                        .minusDays(retentionDays);

        return outboxEventRepository
                .deletePublishedPasswordResetEventsBefore(
                        cutoff
                );
    }
}
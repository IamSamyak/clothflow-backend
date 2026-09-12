package com.clothflow.user.service;

import com.clothflow.user.entity.OutboxEvent;
import com.clothflow.user.kafka.OutboxKafkaPublisher;
import com.clothflow.user.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxPublisherService {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;

    private final OutboxKafkaPublisher outboxKafkaPublisher;

    private final OutboxEventPersistenceService outboxEventPersistenceService;

    @Value("${outbox.publisher.processing-timeout-seconds:60}")
    private long processingTimeoutSeconds;

    public OutboxPublisherService(
            OutboxEventRepository outboxEventRepository,
            OutboxKafkaPublisher outboxKafkaPublisher, OutboxEventPersistenceService outboxEventPersistenceService
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.outboxKafkaPublisher =
                outboxKafkaPublisher;
        this.outboxEventPersistenceService = outboxEventPersistenceService;
    }

    @Transactional
    public List<OutboxEvent> claimPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findPendingForUpdate(
                        PageRequest.of(
                                0,
                                BATCH_SIZE
                        )
                );

        events.forEach(
                OutboxEvent::markProcessing
        );

        outboxEventRepository.saveAll(
                events
        );

        return events;
    }

    public void publish(
            OutboxEvent event
    ) {

        outboxKafkaPublisher
                .publish(event)
                .whenComplete(
                        (result, exception) -> {

                            if (exception == null) {

                                outboxEventPersistenceService
                                        .markPublished(
                                                event.getId()
                                        );

                            } else {

                                /*
                                 * Retry handling will be implemented
                                 * in the next step.
                                 */
                            }
                        }
                );
    }

    @Transactional
    public int recoverExpiredEvents() {

        OffsetDateTime cutoff =
                OffsetDateTime.now()
                        .minusSeconds(
                                processingTimeoutSeconds
                        );

        return outboxEventRepository
                .releaseExpiredProcessingEvents(
                        cutoff
                );
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.entity.OutboxEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OutboxPublisher {

    private final OutboxClaimService outboxClaimService;

    private final OutboxStatusService outboxStatusService;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String inventoryEventsTopic;

    public OutboxPublisher(
            OutboxClaimService outboxClaimService,
            OutboxStatusService outboxStatusService,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${clothflow.kafka.topics.inventory-events}")
            String inventoryEventsTopic) {

        this.outboxClaimService =
                outboxClaimService;

        this.outboxStatusService =
                outboxStatusService;

        this.kafkaTemplate =
                kafkaTemplate;

        this.inventoryEventsTopic =
                inventoryEventsTopic;
    }

    @Scheduled(
            fixedDelayString =
                    "${clothflow.outbox.publisher.fixed-delay-ms}"
    )
    public void publishPendingEventsScheduled() {

        publishPendingEvents();
    }

    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxClaimService
                        .claimPendingEvents();

        for (OutboxEvent event : events) {

            try {

                kafkaTemplate.send(
                        inventoryEventsTopic,
                        event.getAggregateId().toString(),
                        event.getPayload()
                ).get();

                outboxStatusService
                        .markPublished(event);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                outboxStatusService
                        .markFailed(
                                event,
                                "Outbox publishing was interrupted"
                        );

            } catch (ExecutionException e) {

                Throwable cause =
                        e.getCause();

                String errorMessage =
                        cause != null
                                ? cause.getMessage()
                                : e.getMessage();

                outboxStatusService
                        .markFailed(
                                event,
                                errorMessage
                        );
            }
        }
    }

    @Scheduled(
            fixedDelayString =
                    "${clothflow.outbox.publisher.recovery-delay-ms}"
    )
    public void recoverStaleEventsScheduled() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(2);

        outboxClaimService
                .recoverStaleEvents(cutoff);
    }
}
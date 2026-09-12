package com.clothflow.user.scheduler;

import com.clothflow.user.entity.OutboxEvent;
import com.clothflow.user.service.OutboxPublisherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherScheduler {

    private final OutboxPublisherService outboxPublisherService;

    public OutboxPublisherScheduler(
            OutboxPublisherService outboxPublisherService
    ) {
        this.outboxPublisherService =
                outboxPublisherService;
    }

    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay:1000}"
    )
    public void publishPendingEvents() {

        outboxPublisherService
                .recoverExpiredEvents();

        List<OutboxEvent> events =
                outboxPublisherService
                        .claimPendingEvents();

        events.forEach(
                outboxPublisherService::publish
        );
    }
}
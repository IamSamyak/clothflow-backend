package com.clothflow.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter notificationsReceived;

    private final Counter notificationsCreated;

    private final Counter duplicateEvents;

    private final Counter processingFailures;

    private final Counter kafkaRetries;

    private final Counter kafkaDlt;

    private final Timer processingTimer;

    public NotificationMetrics(
            MeterRegistry meterRegistry
    ) {

        this.notificationsReceived =
                Counter.builder(
                                "clothflow.notification.received"
                        )
                        .description(
                                "Number of Kafka notification events received"
                        )
                        .register(meterRegistry);

        this.notificationsCreated =
                Counter.builder(
                                "clothflow.notification.created"
                        )
                        .description(
                                "Number of notifications successfully created"
                        )
                        .register(meterRegistry);

        this.duplicateEvents =
                Counter.builder(
                                "clothflow.notification.duplicate"
                        )
                        .description(
                                "Number of duplicate notification events ignored"
                        )
                        .register(meterRegistry);

        this.processingFailures =
                Counter.builder(
                                "clothflow.notification.failed"
                        )
                        .description(
                                "Number of notification event processing failures"
                        )
                        .register(meterRegistry);

        this.kafkaRetries =
                Counter.builder(
                                "clothflow.kafka.retry"
                        )
                        .description(
                                "Number of Kafka notification delivery retries"
                        )
                        .register(meterRegistry);

        this.kafkaDlt =
                Counter.builder(
                                "clothflow.kafka.dlt"
                        )
                        .description(
                                "Number of notification events recovered to a Kafka dead-letter topic"
                        )
                        .register(meterRegistry);

        this.processingTimer =
                Timer.builder(
                                "clothflow.notification.processing"
                        )
                        .description(
                                "Time taken to process notification events"
                        )
                        .publishPercentileHistogram()
                        .register(meterRegistry);
    }

    public void notificationReceived() {
        notificationsReceived.increment();
    }

    public void notificationCreated() {
        notificationsCreated.increment();
    }

    public void duplicateEvent() {
        duplicateEvents.increment();
    }

    public void processingFailure() {
        processingFailures.increment();
    }

    public void kafkaRetry() {
        kafkaRetries.increment();
    }

    public void kafkaDlt() {
        kafkaDlt.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }

    public void recordProcessing(
            Timer.Sample sample
    ) {
        sample.stop(processingTimer);
    }
}
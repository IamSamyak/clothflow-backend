package com.clothflow.user.kafka;

import com.clothflow.user.entity.OutboxEvent;
import com.clothflow.user.security.OutboxPayloadEncryptionService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.nio.charset.StandardCharsets;

@Component
public class OutboxKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final OutboxPayloadEncryptionService outboxPayloadEncryptionService;

    public OutboxKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate, OutboxPayloadEncryptionService outboxPayloadEncryptionService
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxPayloadEncryptionService = outboxPayloadEncryptionService;
    }

    public CompletableFuture<SendResult<String, String>> publish(
            OutboxEvent event
    ) {

        String topic =
                resolveTopic(
                        event.getEventType()
                );

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        topic,
                        event.getAggregateId().toString(),
                        outboxPayloadEncryptionService.decrypt(
                                event.getPayload()
                        )
                );

        record.headers()
                .add(
                        "event-id",
                        event.getId()
                                .toString()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        record.headers()
                .add(
                        "event-type",
                        event.getEventType()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        return kafkaTemplate.send(record);
    }

    private String resolveTopic(
            String eventType
    ) {

        return switch (eventType) {

            case "PASSWORD_RESET_REQUESTED" ->
                    UserKafkaTopics.PASSWORD_RESET_REQUESTED;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported outbox event type: "
                                    + eventType
                    );
        };
    }
}
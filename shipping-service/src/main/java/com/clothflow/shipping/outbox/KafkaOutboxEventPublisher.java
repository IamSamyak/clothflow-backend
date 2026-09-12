package com.clothflow.shipping.outbox;

import com.clothflow.shipping.entity.OutboxEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class KafkaOutboxEventPublisher
        implements OutboxEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    KafkaOutboxEventPublisher.class
            );

    private static final String SHIPPING_EVENTS_TOPIC =
            "clothflow.shipping.events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.kafkaTemplate =
                kafkaTemplate;
    }

    @Override
    public void publish(
            OutboxEvent event
    ) {

        try {

            SendResult<String, String> result =
                    kafkaTemplate
                            .send(
                                    SHIPPING_EVENTS_TOPIC,
                                    event.getAggregateId()
                                            .toString(),
                                    event.getPayload()
                            )
                            .get();

            RecordMetadata metadata =
                    result.getRecordMetadata();

            log.info(
                    "Kafka event published successfully: " +
                            "eventId={}, aggregateId={}, " +
                            "eventType={}, topic={}, " +
                            "partition={}, offset={}",
                    event.getId(),
                    event.getAggregateId(),
                    event.getEventType(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Kafka publishing interrupted for event "
                            + event.getId(),
                    ex
            );

        } catch (ExecutionException ex) {

            throw new IllegalStateException(
                    "Kafka publishing failed for event "
                            + event.getId(),
                    ex.getCause()
            );
        }
    }
}
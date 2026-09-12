package com.clothflow.order.kafka;

import com.clothflow.order.event.EventEnvelope;
import com.clothflow.order.event.PaymentEventType;
import com.clothflow.order.event.PaymentRefundedEvent;
import com.clothflow.order.event.PaymentSucceededEvent;
import com.clothflow.order.service.OrderPaymentEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final OrderPaymentEventService
            orderPaymentEventService;

    public PaymentEventConsumer(
            ObjectMapper objectMapper,
            OrderPaymentEventService orderPaymentEventService
    ) {

        this.objectMapper =
                objectMapper;

        this.orderPaymentEventService =
                orderPaymentEventService;
    }

    @KafkaListener(
            topics = "clothflow.payment.events",
            groupId = "clothflow-order-service"
    )
    public void consumePaymentEvent(
            String payload,
            Acknowledgment acknowledgment
    ) throws Exception {

        /*
         * First deserialize only the event envelope.
         *
         * We intentionally do NOT inspect the business
         * payload to determine the event type.
         */
        EventEnvelope<JsonNode> envelope =
                objectMapper.readValue(
                        payload,
                        objectMapper.getTypeFactory()
                                .constructParametricType(
                                        EventEnvelope.class,
                                        JsonNode.class
                                )
                );

        log.info(
                "Received payment event: " +
                        "eventId={}, eventType={}, " +
                        "aggregateType={}, aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateType(),
                envelope.aggregateId()
        );

        /*
         * Determine the event using the explicit
         * eventType from the envelope.
         */
        PaymentEventType eventType;

        try {

            eventType =
                    PaymentEventType.valueOf(
                            envelope.eventType()
                    );

        } catch (IllegalArgumentException ex) {

            /*
             * The message contains an unsupported event type.
             *
             * Retrying will not change the message, so this
             * exception is treated as non-retryable by the
             * Kafka error handler.
             */
            throw ex;
        }

        /*
         * Dispatch the event to the appropriate
         * existing Order Service handler.
         */
        switch (eventType) {

            case PAYMENT_SUCCEEDED -> {

                consumePaymentSucceeded(
                        envelope,
                        acknowledgment
                );
            }

            case PAYMENT_REFUNDED -> {

                consumePaymentRefunded(
                        envelope,
                        acknowledgment
                );
            }
        }
    }

    private void consumePaymentSucceeded(
            EventEnvelope<JsonNode> envelope,
            Acknowledgment acknowledgment
    ) throws Exception {

        /*
         * Deserialize only the business payload.
         */
        PaymentSucceededEvent event =
                objectMapper.treeToValue(
                        envelope.payload(),
                        PaymentSucceededEvent.class
                );

        log.info(
                "Processing PAYMENT_SUCCEEDED: " +
                        "eventId={}, orderId={}, " +
                        "paymentId={}, amount={}",
                envelope.eventId(),
                event.orderId(),
                event.paymentId(),
                event.amount()
        );

        /*
         * Event ID comes from the envelope.
         *
         * This ID is used by the Order Service Inbox
         * / ProcessedEvent mechanism for idempotency.
         */
        boolean newlyProcessed =
                orderPaymentEventService
                        .handlePaymentSucceeded(
                                envelope.eventId(),
                                event
                        );

        /*
         * ACK only after the Order Service transaction
         * has completed successfully.
         */
        acknowledgment.acknowledge();

        if (newlyProcessed) {

            log.info(
                    "Successfully processed PAYMENT_SUCCEEDED: " +
                            "eventId={}, orderId={}",
                    envelope.eventId(),
                    event.orderId()
            );

        } else {

            log.info(
                    "Duplicate PAYMENT_SUCCEEDED ignored: " +
                            "eventId={}, orderId={}",
                    envelope.eventId(),
                    event.orderId()
            );
        }
    }

    private void consumePaymentRefunded(
            EventEnvelope<JsonNode> envelope,
            Acknowledgment acknowledgment
    ) throws Exception {

        /*
         * Deserialize only the business payload.
         */
        PaymentRefundedEvent event =
                objectMapper.treeToValue(
                        envelope.payload(),
                        PaymentRefundedEvent.class
                );

        log.info(
                "Processing PAYMENT_REFUNDED: " +
                        "eventId={}, refundId={}, " +
                        "paymentId={}, orderId={}, amount={}",
                envelope.eventId(),
                event.refundId(),
                event.paymentId(),
                event.orderId(),
                event.amount()
        );

        /*
         * Event ID comes from the envelope.
         *
         * Existing refund Saga handling is preserved.
         */
        boolean newlyProcessed =
                orderPaymentEventService
                        .handlePaymentRefunded(
                                envelope.eventId(),
                                event
                        );

        /*
         * ACK only after successful processing.
         */
        acknowledgment.acknowledge();

        if (newlyProcessed) {

            log.info(
                    "Successfully processed PAYMENT_REFUNDED: " +
                            "eventId={}, orderId={}, refundId={}",
                    envelope.eventId(),
                    event.orderId(),
                    event.refundId()
            );

        } else {

            log.info(
                    "Duplicate PAYMENT_REFUNDED ignored: " +
                            "eventId={}, orderId={}, refundId={}",
                    envelope.eventId(),
                    event.orderId(),
                    event.refundId()
            );
        }
    }
}
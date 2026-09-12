package com.clothflow.payment.service;

import com.clothflow.payment.entity.OutboxEvent;
import com.clothflow.payment.entity.Payment;
import com.clothflow.payment.entity.PaymentRefund;
import com.clothflow.payment.event.EventEnvelope;
import com.clothflow.payment.event.PaymentEventType;
import com.clothflow.payment.event.PaymentRefundedEvent;
import com.clothflow.payment.event.PaymentSucceededEvent;
import com.clothflow.payment.repository.OutboxEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void createPaymentSucceededEvent(
            Payment payment
    ) {

        /*
         * Business payload.
         *
         * Event metadata such as eventId, eventType,
         * aggregateId and occurredAt belongs to the envelope.
         */
        PaymentSucceededEvent payload =
                new PaymentSucceededEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getPaymentReference(),
                        payment.getPaymentMethod().name(),
                        OffsetDateTime.now()
                );

        /*
         * Generate a unique event ID.
         *
         * This ID is later used by the Order Service
         * Inbox / ProcessedEvent mechanism for
         * duplicate-event protection.
         */
        UUID eventId = UUID.randomUUID();

        OffsetDateTime occurredAt =
                OffsetDateTime.now();

        /*
         * Wrap the business payload inside the
         * standard event envelope.
         */
        EventEnvelope<PaymentSucceededEvent> envelope =
                new EventEnvelope<>(
                        eventId,
                        PaymentEventType.PAYMENT_SUCCEEDED.name(),
                        "PAYMENT",
                        payment.getId(),
                        occurredAt,
                        payload
                );

        try {

            String serializedEvent =
                    objectMapper.writeValueAsString(envelope);

            /*
             * Store the complete envelope in the
             * transactional outbox.
             */
            OutboxEvent outboxEvent =
                    new OutboxEvent(
                            payment.getId(),
                            "PAYMENT",
                            PaymentEventType.PAYMENT_SUCCEEDED.name(),
                            serializedEvent
                    );

            outboxEventRepository.save(outboxEvent);

        } catch (JacksonException ex) {

            throw new IllegalStateException(
                    "Failed to serialize payment succeeded event",
                    ex
            );
        }
    }

    public void createPaymentRefundedEvent(
            Payment payment,
            PaymentRefund refund
    ) {

        /*
         * Business payload for the refund event.
         *
         * Event metadata is kept in EventEnvelope.
         */
        PaymentRefundedEvent payload =
                new PaymentRefundedEvent(
                        refund.getId(),
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        refund.getAmount(),
                        refund.getCurrency(),
                        refund.getRefundReference()
                );

        /*
         * Every published event gets its own
         * unique event ID.
         */
        UUID eventId = UUID.randomUUID();

        OffsetDateTime occurredAt =
                OffsetDateTime.now();

        /*
         * Create the standard event envelope.
         */
        EventEnvelope<PaymentRefundedEvent> envelope =
                new EventEnvelope<>(
                        eventId,
                        PaymentEventType.PAYMENT_REFUNDED.name(),
                        "PAYMENT",
                        payment.getId(),
                        occurredAt,
                        payload
                );

        try {

            String serializedEvent =
                    objectMapper.writeValueAsString(envelope);

            /*
             * Store the complete envelope in the
             * transactional outbox.
             */
            OutboxEvent outboxEvent =
                    new OutboxEvent(
                            payment.getId(),
                            "PAYMENT",
                            PaymentEventType.PAYMENT_REFUNDED.name(),
                            serializedEvent
                    );

            outboxEventRepository.save(outboxEvent);

        } catch (JacksonException ex) {

            throw new IllegalStateException(
                    "Failed to serialize payment refunded event",
                    ex
            );
        }
    }
}

package com.clothflow.order.outbox;

import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderItem;
import com.clothflow.order.entity.OutboxEvent;
import com.clothflow.order.event.EventEnvelope;
import com.clothflow.order.event.OrderConfirmedEvent;
import com.clothflow.order.event.OrderConfirmedItem;
import com.clothflow.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderOutboxService {

    private static final String AGGREGATE_TYPE = "ORDER";

    private static final String EVENT_TYPE =
            "ORDER_CONFIRMED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createOrderConfirmedEvent(
            Order order
    ) {

        UUID eventId = UUID.randomUUID();

        List<OrderConfirmedItem> items =
                order.getItems()
                        .stream()
                        .map(this::toEventItem)
                        .toList();

        OrderConfirmedEvent payload =
                new OrderConfirmedEvent(
                        order.getId(),
                        order.getCustomerId(),
                        order.getRecipientName(),
                        order.getShippingAddressLine1(),
                        order.getShippingAddressLine2(),
                        order.getShippingCity(),
                        order.getShippingState(),
                        order.getShippingPostalCode(),
                        order.getShippingCountry(),
                        items
                );

        EventEnvelope<OrderConfirmedEvent> envelope =
                new EventEnvelope<>(
                        eventId,
                        EVENT_TYPE,
                        AGGREGATE_TYPE,
                        order.getId(),
                        OffsetDateTime.now(),
                        payload
                );

        try {

            String serializedPayload =
                    objectMapper.writeValueAsString(
                            envelope
                    );

            OutboxEvent outboxEvent =
                    new OutboxEvent(
                            order.getId(),
                            AGGREGATE_TYPE,
                            EVENT_TYPE,
                            serializedPayload
                    );

            // Preserve the envelope event ID as the outbox ID.
            // This makes event identity consistent across
            // outbox -> Kafka -> consumer inbox.
            outboxEvent.setId(eventId);

            outboxEventRepository.save(
                    outboxEvent
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to serialize ORDER_CONFIRMED event "
                            + "for order " + order.getId(),
                    ex
            );
        }
    }

    private OrderConfirmedItem toEventItem(
            OrderItem item
    ) {
        return new OrderConfirmedItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity()
        );
    }
}
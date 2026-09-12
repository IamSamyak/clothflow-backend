package com.clothflow.shipping.outbox;

import com.clothflow.shipping.entity.OutboxEvent;
import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentItem;
import com.clothflow.shipping.event.EventEnvelope;
import com.clothflow.shipping.event.ShipmentCreatedEvent;
import com.clothflow.shipping.event.ShipmentCreatedItem;
import com.clothflow.shipping.event.ShipmentLifecycleEvent;
import com.clothflow.shipping.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ShippingOutboxService {

    private static final String AGGREGATE_TYPE =
            "SHIPMENT";

    private static final String EVENT_TYPE =
            "SHIPMENT_CREATED";

    private final OutboxEventRepository
            outboxEventRepository;

    private final ObjectMapper objectMapper;

    public ShippingOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository =
                outboxEventRepository;

        this.objectMapper =
                objectMapper;
    }

    @Transactional
    public void createShipmentCreatedEvent(
            Shipment shipment
    ) {

        UUID eventId =
                UUID.randomUUID();

        List<ShipmentCreatedItem> items =
                shipment.getItems()
                        .stream()
                        .map(this::toEventItem)
                        .toList();

        ShipmentCreatedEvent payload =
                new ShipmentCreatedEvent(
                        shipment.getId(),
                        shipment.getOrderId(),
                        shipment.getCustomerId(),
                        shipment.getRecipientName(),
                        shipment.getAddressLine1(),
                        shipment.getAddressLine2(),
                        shipment.getCity(),
                        shipment.getState(),
                        shipment.getPostalCode(),
                        shipment.getCountry(),
                        items
                );

        EventEnvelope<ShipmentCreatedEvent>
                envelope =
                new EventEnvelope<>(
                        eventId,
                        EVENT_TYPE,
                        AGGREGATE_TYPE,
                        shipment.getId(),
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
                            shipment.getId(),
                            AGGREGATE_TYPE,
                            EVENT_TYPE,
                            serializedPayload
                    );

            /*
             * The Kafka event ID and the outbox row ID
             * are deliberately identical.
             *
             * This makes event identity deterministic
             * throughout the publishing pipeline.
             */
            outboxEvent.setId(eventId);

            outboxEventRepository.save(
                    outboxEvent
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to serialize " +
                            "SHIPMENT_CREATED event for shipment "
                            + shipment.getId(),
                    ex
            );
        }
    }

    private ShipmentCreatedItem toEventItem(
            ShipmentItem item
    ) {

        return new ShipmentCreatedItem(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity()
        );
    }

    @Transactional
    public void createShipmentLifecycleEvent(
            Shipment shipment,
            String eventType
    ) {

        UUID eventId =
                UUID.randomUUID();

        ShipmentLifecycleEvent payload =
                new ShipmentLifecycleEvent(
                        shipment.getId(),
                        shipment.getOrderId(),
                        shipment.getCustomerId(),
                        shipment.getStatus(),
                        shipment.getCarrier(),
                        shipment.getTrackingNumber(),
                        OffsetDateTime.now()
                );

        EventEnvelope<ShipmentLifecycleEvent>
                envelope =
                new EventEnvelope<>(
                        eventId,
                        eventType,
                        "SHIPMENT",
                        shipment.getId(),
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
                            shipment.getId(),
                            "SHIPMENT",
                            eventType,
                            serializedPayload
                    );

            /*
             * Event ID and outbox ID remain identical.
             */
            outboxEvent.setId(eventId);

            outboxEventRepository.save(
                    outboxEvent
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to serialize "
                            + eventType
                            + " event for shipment "
                            + shipment.getId(),
                    ex
            );
        }
    }
}
package com.clothflow.order.kafka;

import com.clothflow.order.event.EventEnvelope;
import com.clothflow.order.event.ShipmentCreatedEvent;
import com.clothflow.order.event.ShipmentLifecycleEvent;
import com.clothflow.order.event.ShippingEventType;
import com.clothflow.order.service.OrderShippingEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ShippingEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShippingEventConsumer.class
            );

    private final ObjectMapper objectMapper;

    private final OrderShippingEventService
            orderShippingEventService;

    public ShippingEventConsumer(
            ObjectMapper objectMapper,
            OrderShippingEventService
                    orderShippingEventService
    ) {
        this.objectMapper =
                objectMapper;

        this.orderShippingEventService =
                orderShippingEventService;
    }

    @KafkaListener(
            topics = "clothflow.shipping.events",
            groupId = "clothflow-order-service"
    )
    public void consumeShippingEvent(
            String payload,
            Acknowledgment acknowledgment
    ) throws Exception {

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
                "Received shipping event: " +
                        "eventId={}, eventType={}, " +
                        "aggregateType={}, aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateType(),
                envelope.aggregateId()
        );

        ShippingEventType eventType;

        try {

            eventType =
                    ShippingEventType.valueOf(
                            envelope.eventType()
                    );

        } catch (IllegalArgumentException ex) {

            throw ex;
        }

        switch (eventType) {

            case SHIPMENT_CREATED -> {

                ShipmentCreatedEvent event =
                        objectMapper.treeToValue(
                                envelope.payload(),
                                ShipmentCreatedEvent.class
                        );

                boolean newlyProcessed =
                        orderShippingEventService
                                .handleShipmentCreatedEvent(
                                        envelope.eventId(),
                                        envelope.eventType(),
                                        envelope.aggregateId(),
                                        event
                                );

                acknowledgment.acknowledge();

                if (newlyProcessed) {

                    log.info(
                            "Successfully processed SHIPMENT_CREATED: " +
                                    "eventId={}, shipmentId={}, orderId={}",
                            envelope.eventId(),
                            event.shipmentId(),
                            event.orderId()
                    );

                } else {

                    log.info(
                            "Duplicate SHIPMENT_CREATED ignored: " +
                                    "eventId={}, shipmentId={}, orderId={}",
                            envelope.eventId(),
                            event.shipmentId(),
                            event.orderId()
                    );
                }
            }

            case SHIPMENT_SHIPPED,
                 SHIPMENT_OUT_FOR_DELIVERY,
                 SHIPMENT_DELIVERED,
                 SHIPMENT_DELIVERY_FAILED -> {

                consumeShipmentLifecycleEvent(
                        envelope,
                        acknowledgment
                );
            }
        }
    }

    private void consumeShipmentLifecycleEvent(
            EventEnvelope<JsonNode> envelope,
            Acknowledgment acknowledgment
    ) throws Exception {

        ShipmentLifecycleEvent event =
                objectMapper.treeToValue(
                        envelope.payload(),
                        ShipmentLifecycleEvent.class
                );

        boolean newlyProcessed =
                orderShippingEventService
                        .handleShipmentLifecycleEvent(
                                envelope.eventId(),
                                envelope.eventType(),
                                envelope.aggregateId(),
                                event
                        );

        acknowledgment.acknowledge();

        if (newlyProcessed) {

            log.info(
                    "Successfully processed shipping event: " +
                            "eventId={}, orderId={}, " +
                            "eventType={}",
                    envelope.eventId(),
                    event.orderId(),
                    envelope.eventType()
            );

        } else {

            log.info(
                    "Duplicate shipping event ignored: " +
                            "eventId={}, orderId={}, " +
                            "eventType={}",
                    envelope.eventId(),
                    event.orderId(),
                    envelope.eventType()
            );
        }
    }
}
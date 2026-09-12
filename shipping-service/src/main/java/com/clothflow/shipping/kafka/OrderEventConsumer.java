package com.clothflow.shipping.kafka;

import com.clothflow.shipping.event.EventEnvelope;
import com.clothflow.shipping.event.OrderConfirmedEvent;
import com.clothflow.shipping.service.ProcessedEventService;
import com.clothflow.shipping.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderEventConsumer.class
            );

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final ShipmentService shipmentService;

    public OrderEventConsumer(
            ObjectMapper objectMapper,
            ProcessedEventService processedEventService,
            ShipmentService shipmentService
    ) {
        this.objectMapper = objectMapper;
        this.processedEventService =
                processedEventService;
        this.shipmentService =
                shipmentService;
    }

    @KafkaListener(
            topics = "clothflow.order.events",
            groupId = "clothflow-shipping-service"
    )
    public void consumeOrderEvent(
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
                "Received order event: " +
                        "eventId={}, eventType={}, " +
                        "aggregateType={}, aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateType(),
                envelope.aggregateId()
        );

        if (!"ORDER".equals(
                envelope.aggregateType()
        )) {

            throw new IllegalArgumentException(
                    "ORDER_CONFIRMED event must have " +
                            "aggregateType ORDER"
            );
        }

        switch (envelope.eventType()) {

            case "ORDER_CONFIRMED" -> {

                OrderConfirmedEvent event =
                        objectMapper.treeToValue(
                                envelope.payload(),
                                OrderConfirmedEvent.class
                        );

                boolean newlyProcessed =
                        shipmentService.handleOrderConfirmed(
                                envelope,
                                event
                        );

                /*
                 * ACK only after the complete database
                 * transaction has succeeded.
                 */
                acknowledgment.acknowledge();

                if (newlyProcessed) {

                    log.info(
                            "Successfully processed " +
                                    "ORDER_CONFIRMED: " +
                                    "eventId={}, orderId={}",
                            envelope.eventId(),
                            event.orderId()
                    );

                } else {

                    log.info(
                            "Duplicate ORDER_CONFIRMED ignored: " +
                                    "eventId={}, orderId={}",
                            envelope.eventId(),
                            event.orderId()
                    );
                }
            }

            default -> {

                throw new IllegalArgumentException(
                        "Unsupported order event type: "
                                + envelope.eventType()
                );
            }
        }
    }
}
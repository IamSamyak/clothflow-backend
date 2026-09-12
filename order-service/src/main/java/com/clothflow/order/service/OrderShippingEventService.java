package com.clothflow.order.service;

import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderStatus;
import com.clothflow.order.event.ShipmentCreatedEvent;
import com.clothflow.order.event.ShipmentLifecycleEvent;
import com.clothflow.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderShippingEventService {

    private final ProcessedEventService processedEventService;
    private final OrderRepository orderRepository;
    private final OrderStatePersistenceService orderStatePersistenceService;

    public OrderShippingEventService(
            ProcessedEventService processedEventService,
            OrderRepository orderRepository,
            OrderStatePersistenceService orderStatePersistenceService
    ) {
        this.processedEventService =
                processedEventService;

        this.orderRepository =
                orderRepository;

        this.orderStatePersistenceService =
                orderStatePersistenceService;
    }

    @Transactional
    public boolean handleShipmentLifecycleEvent(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            ShipmentLifecycleEvent event
    ) {

        boolean newlyProcessed =
                processedEventService.tryMarkProcessed(
                        eventId,
                        eventType,
                        aggregateId
                );

        if (!newlyProcessed) {
            return false;
        }

        if (!aggregateId.equals(event.shipmentId())) {
            throw new IllegalStateException(
                    "Shipping event aggregateId does not " +
                            "match shipmentId"
            );
        }

        Order order =
                orderRepository.findById(
                        event.orderId()
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "Order not found for shipping event: "
                                        + event.orderId()
                        )
                );

        switch (eventType) {

            case "SHIPMENT_SHIPPED" -> {

                if (order.getStatus() ==
                        OrderStatus.PROCESSING) {

                    orderStatePersistenceService
                            .markShipped(order.getId());

                } else if (
                        order.getStatus() ==
                                OrderStatus.SHIPPED
                                ||
                                order.getStatus() ==
                                        OrderStatus.DELIVERED
                ) {

                    // Duplicate or stale event.
                    //
                    // Nothing to do.

                } else {

                    throw new IllegalStateException(
                            "Cannot process SHIPMENT_SHIPPED " +
                                    "for order " +
                                    order.getId() +
                                    " in status " +
                                    order.getStatus()
                    );
                }
            }

            case "SHIPMENT_DELIVERED" -> {

                if (order.getStatus() ==
                        OrderStatus.SHIPPED) {

                    orderStatePersistenceService
                            .markDelivered(order.getId());

                } else if (
                        order.getStatus() ==
                                OrderStatus.DELIVERED
                ) {

                    // Duplicate event.
                    //
                    // Nothing to do.

                } else {

                    throw new IllegalStateException(
                            "Cannot process SHIPMENT_DELIVERED " +
                                    "for order " +
                                    order.getId() +
                                    " in status " +
                                    order.getStatus()
                    );
                }
            }

            case "SHIPMENT_OUT_FOR_DELIVERY" -> {

                // Order does not currently own an
                // OUT_FOR_DELIVERY state.
                //
                // Shipping owns this state.
            }

            case "SHIPMENT_DELIVERY_FAILED" -> {

                // Shipping owns delivery failure.
                //
                // No Order state transition currently exists.
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported shipping event type: "
                            + eventType
            );
        }

        return true;
    }

    @Transactional
    public boolean handleShipmentCreatedEvent(
            UUID eventId,
            String eventType,
            UUID aggregateId,
            ShipmentCreatedEvent event
    ) {

        boolean newlyProcessed =
                processedEventService.tryMarkProcessed(
                        eventId,
                        eventType,
                        aggregateId
                );

        if (!newlyProcessed) {
            return false;
        }

        if (!aggregateId.equals(event.shipmentId())) {
            throw new IllegalStateException(
                    "SHIPMENT_CREATED aggregateId does not " +
                            "match shipmentId"
            );
        }

        Order order =
                orderRepository.findById(event.orderId())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Order not found for shipment-created event: "
                                                + event.orderId()
                                )
                        );

        order.assignShipment(event.shipmentId());

        orderRepository.saveAndFlush(order);

        return true;
    }
}
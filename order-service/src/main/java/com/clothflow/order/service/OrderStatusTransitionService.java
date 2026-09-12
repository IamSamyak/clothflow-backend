package com.clothflow.order.service;

import com.clothflow.order.entity.OrderStatus;
import com.clothflow.order.exception.InvalidOrderStatusTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusTransitionService {

    private final Map<OrderStatus, Set<OrderStatus>> allowedTransitions;

    public OrderStatusTransitionService() {

        Map<OrderStatus, Set<OrderStatus>> transitions =
                new EnumMap<>(OrderStatus.class);

        transitions.put(
                OrderStatus.PENDING,
                EnumSet.of(
                        OrderStatus.INVENTORY_RESERVED,
                        OrderStatus.INVENTORY_FAILED,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.INVENTORY_RESERVED,
                EnumSet.of(
                        OrderStatus.PAYMENT_PENDING,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.PAYMENT_PENDING,
                EnumSet.of(
                        OrderStatus.CONFIRMED,
                        OrderStatus.PAYMENT_FAILED
                )
        );

        transitions.put(
                OrderStatus.CONFIRMED,
                EnumSet.of(
                        OrderStatus.PROCESSING,
                        OrderStatus.CANCELLED
                )
        );

        transitions.put(
                OrderStatus.PROCESSING,
                EnumSet.of(OrderStatus.SHIPPED)
        );

        transitions.put(
                OrderStatus.SHIPPED,
                EnumSet.of(OrderStatus.DELIVERED)
        );

        transitions.put(
                OrderStatus.DELIVERED,
                EnumSet.noneOf(OrderStatus.class)
        );

        transitions.put(
                OrderStatus.CANCELLED,
                EnumSet.noneOf(OrderStatus.class)
        );

        transitions.put(
                OrderStatus.INVENTORY_FAILED,
                EnumSet.noneOf(OrderStatus.class)
        );

        transitions.put(
                OrderStatus.PAYMENT_FAILED,
                EnumSet.noneOf(OrderStatus.class)
        );

        this.allowedTransitions = transitions;
    }

    public void validateTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        if (currentStatus == newStatus) {
            throw new InvalidOrderStatusTransitionException(
                    currentStatus,
                    newStatus
            );
        }

        Set<OrderStatus> allowedStatuses =
                allowedTransitions.getOrDefault(
                        currentStatus,
                        EnumSet.noneOf(OrderStatus.class)
                );

        if (!allowedStatuses.contains(newStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    currentStatus,
                    newStatus
            );
        }
    }
}
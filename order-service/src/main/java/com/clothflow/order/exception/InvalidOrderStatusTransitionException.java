package com.clothflow.order.exception;

import com.clothflow.order.entity.OrderStatus;

public class InvalidOrderStatusTransitionException
        extends RuntimeException {

    public InvalidOrderStatusTransitionException(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        super(
                "Invalid order status transition: "
                        + currentStatus
                        + " -> "
                        + newStatus
        );
    }
}
package com.clothflow.order.dto.request;

import com.clothflow.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status,

        String reason
) {
}
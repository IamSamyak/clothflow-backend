package com.clothflow.order.dto.response;

import com.clothflow.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

        UUID id,

        String orderNumber,

        UUID customerId,

        OrderStatus status,

        BigDecimal totalAmount,

        String currency,

        List<OrderItemResponse> items,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt
) {
}
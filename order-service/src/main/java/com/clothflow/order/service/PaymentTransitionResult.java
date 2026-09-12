package com.clothflow.order.service;

import com.clothflow.order.dto.response.OrderResponse;

public record PaymentTransitionResult(
        OrderResponse order,
        boolean transitioned
) {
}
package com.clothflow.order.client;

import com.clothflow.order.dto.response.PaymentResponse;
import com.clothflow.order.entity.Order;

public interface PaymentClient {

    PaymentResponse createPayment(
            Order order,
            String idempotencyKey
    );
}
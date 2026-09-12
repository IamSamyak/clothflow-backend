package com.clothflow.payment.gateway;

import com.clothflow.payment.entity.Payment;

public interface PaymentGateway {

    PaymentGatewayResult process(
            Payment payment
    );
}
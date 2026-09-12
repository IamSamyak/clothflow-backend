package com.clothflow.payment.gateway;

import com.clothflow.payment.entity.PaymentRefund;

public interface PaymentRefundGateway {

    PaymentRefundGatewayResult refund(
            PaymentRefund refund
    );
}
package com.clothflow.payment.gateway;

import com.clothflow.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FakePaymentGateway
        implements PaymentGateway {

    @Override
    public PaymentGatewayResult process(
            Payment payment
    ) {

        /*
         * Deterministic local testing:
         *
         * Amount ending in .99 -> failure
         * Everything else -> success
         */

        if (payment.getAmount()
                .remainder(
                        new java.math.BigDecimal("1.00")
                )
                .compareTo(
                        new java.math.BigDecimal("0.99")
                ) == 0) {

            return PaymentGatewayResult.failure(
                    "FAKE_GATEWAY_DECLINED"
            );
        }

        String gatewayReference =
                "FAKE-" +
                        UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

        return PaymentGatewayResult.success(
                gatewayReference
        );
    }
}
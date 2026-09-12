package com.clothflow.payment.gateway;

import com.clothflow.payment.entity.PaymentRefund;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentRefundGateway
        implements PaymentRefundGateway {

    /*
     * Deterministic refund testing:
     *
     * ₹888.88:
     *     First refund attempt  -> FAILURE
     *     Retry                 -> SUCCESS
     *
     * Other amounts:
     *     SUCCESS
     */
    private static final BigDecimal REFUND_RETRY_TEST_AMOUNT =
            new BigDecimal("888.88");

    /*
     * Keeps track of refund IDs that have already experienced
     * the simulated temporary failure.
     *
     * Thread-safe for concurrent local testing.
     */
    private final Set<UUID> failedOnceRefunds =
            ConcurrentHashMap.newKeySet();

    @Override
    public PaymentRefundGatewayResult refund(
            PaymentRefund refund
    ) {

        /*
         * Special test scenario:
         *
         * First attempt for ₹888.88 -> FAIL
         * Subsequent attempt       -> SUCCESS
         */
        if (refund.getAmount()
                .compareTo(REFUND_RETRY_TEST_AMOUNT) == 0) {

            boolean firstAttempt =
                    failedOnceRefunds.add(refund.getId());

            if (firstAttempt) {

                return PaymentRefundGatewayResult.failure(
                        "FAKE_REFUND_GATEWAY_TEMPORARY_FAILURE"
                );
            }
        }

        /*
         * Successful refund.
         */
        String refundReference =
                "REF-" +
                        UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

        return PaymentRefundGatewayResult.success(
                refundReference
        );
    }
}
package com.clothflow.payment.gateway;

public record PaymentRefundGatewayResult(
        boolean successful,
        String refundReference,
        String failureReason
) {

    public static PaymentRefundGatewayResult success(
            String refundReference
    ) {

        return new PaymentRefundGatewayResult(
                true,
                refundReference,
                null
        );
    }

    public static PaymentRefundGatewayResult failure(
            String failureReason
    ) {

        return new PaymentRefundGatewayResult(
                false,
                null,
                failureReason
        );
    }
}
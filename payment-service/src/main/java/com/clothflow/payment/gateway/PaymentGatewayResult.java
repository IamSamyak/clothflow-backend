package com.clothflow.payment.gateway;

public record PaymentGatewayResult(

        boolean successful,

        String gatewayReference,

        String failureReason
) {

    public static PaymentGatewayResult success(
            String gatewayReference
    ) {
        return new PaymentGatewayResult(
                true,
                gatewayReference,
                null
        );
    }

    public static PaymentGatewayResult failure(
            String failureReason
    ) {
        return new PaymentGatewayResult(
                false,
                null,
                failureReason
        );
    }
}
package com.clothflow.payment.exception;

public class PaymentAlreadyExistsException
        extends RuntimeException {

    public PaymentAlreadyExistsException(
            String idempotencyKey
    ) {
        super(
                "Payment already exists for idempotency key: "
                        + idempotencyKey
        );
    }
}
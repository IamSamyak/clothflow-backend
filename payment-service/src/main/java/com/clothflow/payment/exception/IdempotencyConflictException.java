package com.clothflow.payment.exception;

public class IdempotencyConflictException
        extends RuntimeException {

    public IdempotencyConflictException(
            String idempotencyKey
    ) {
        super(
                "Idempotency key has already been used "
                        + "with a different payment request: "
                        + idempotencyKey
        );
    }
}
package com.clothflow.payment.exception;

import java.util.UUID;

public class RefundAlreadyExistsException
        extends RuntimeException {

    public RefundAlreadyExistsException(
            UUID paymentId
    ) {
        super(
                "A refund already exists for payment: "
                        + paymentId
        );
    }
}
package com.clothflow.payment.service;

import com.clothflow.payment.entity.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentStateTransitionService {

    private static final Map<
            PaymentStatus,
            Set<PaymentStatus>
            > ALLOWED_TRANSITIONS = Map.of(

            PaymentStatus.INITIATED,
            EnumSet.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.CANCELLED
            ),

            PaymentStatus.PENDING,
            EnumSet.of(
                    PaymentStatus.SUCCEEDED,
                    PaymentStatus.FAILED,
                    PaymentStatus.CANCELLED
            ),

            PaymentStatus.SUCCEEDED,
            EnumSet.of(
                    PaymentStatus.REFUND_PENDING
            ),

            PaymentStatus.FAILED,
            EnumSet.noneOf(PaymentStatus.class),

            PaymentStatus.CANCELLED,
            EnumSet.noneOf(PaymentStatus.class),

            PaymentStatus.REFUND_PENDING,
            EnumSet.of(
                    PaymentStatus.REFUNDED
            ),

            PaymentStatus.REFUNDED,
            EnumSet.noneOf(PaymentStatus.class)
    );

    public void validateTransition(
            PaymentStatus currentStatus,
            PaymentStatus newStatus
    ) {

        if (currentStatus == newStatus) {
            throw new IllegalStateException(
                    "Payment is already in status "
                            + currentStatus
            );
        }

        Set<PaymentStatus> allowedStatuses =
                ALLOWED_TRANSITIONS.get(currentStatus);

        if (allowedStatuses == null
                || !allowedStatuses.contains(newStatus)) {

            throw new IllegalStateException(
                    "Invalid payment status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }
}
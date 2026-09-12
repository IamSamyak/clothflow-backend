package com.clothflow.payment.service;

import com.clothflow.payment.dto.response.PaymentRefundResponse;
import com.clothflow.payment.entity.Payment;
import com.clothflow.payment.entity.PaymentRefund;
import com.clothflow.payment.entity.PaymentRefundStatus;
import com.clothflow.payment.entity.PaymentStatus;
import com.clothflow.payment.exception.IdempotencyConflictException;
import com.clothflow.payment.exception.PaymentNotFoundException;
import com.clothflow.payment.exception.RefundAlreadyExistsException;
import com.clothflow.payment.gateway.PaymentRefundGateway;
import com.clothflow.payment.gateway.PaymentRefundGatewayResult;
import com.clothflow.payment.repository.PaymentRefundRepository;
import com.clothflow.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentRefundService {

    private final PaymentRepository paymentRepository;

    private final PaymentRefundRepository paymentRefundRepository;

    private final PaymentRefundGateway paymentRefundGateway;

    private final PaymentStateTransitionService stateTransitionService;

    private final PaymentEventService paymentEventService;

    public PaymentRefundService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            PaymentRefundGateway paymentRefundGateway,
            PaymentStateTransitionService stateTransitionService, PaymentEventService paymentEventService
    ) {
        this.paymentRepository =
                paymentRepository;

        this.paymentRefundRepository =
                paymentRefundRepository;

        this.paymentRefundGateway =
                paymentRefundGateway;

        this.stateTransitionService =
                stateTransitionService;
        this.paymentEventService = paymentEventService;
    }

    @Transactional
    public PaymentRefundResponse refundPayment(
            UUID paymentId,
            String idempotencyKey
    ) {

        validateIdempotencyKey(idempotencyKey);

        /*
         * Same idempotency key means the caller is
         * retrying the same logical refund request.
         *
         * Do NOT create another refund.
         */
        PaymentRefund existingRefund =
                paymentRefundRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
                        .orElse(null);

        if (existingRefund != null) {

            if (!existingRefund.matchesRequest(paymentId)) {

                throw new IdempotencyConflictException(
                        idempotencyKey
                );
            }

            return toResponse(existingRefund);
        }

        /*
         * Lock the payment before changing its state.
         */
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(
                                () -> new PaymentNotFoundException(
                                        paymentId
                                )
                        );

        /*
         * The original payment must have succeeded.
         */
        if (payment.getStatus()
                != PaymentStatus.SUCCEEDED) {

            throw new IllegalStateException(
                    "Payment cannot be refunded from status "
                            + payment.getStatus()
            );
        }

        /*
         * Only one refund operation is allowed
         * for a payment.
         */
        PaymentRefund existingPaymentRefund =
                paymentRefundRepository
                        .findByPaymentId(paymentId)
                        .orElse(null);

        if (existingPaymentRefund != null) {

            throw new RefundAlreadyExistsException(
                    paymentId
            );
        }

        /*
         * Create the refund operation.
         */
        PaymentRefund refund =
                new PaymentRefund(
                        payment.getId(),
                        payment.getOrderId(),
                        generateRefundReference(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        idempotencyKey
                );

        paymentRefundRepository.saveAndFlush(
                refund
        );

        /*
         * Payment is now waiting for the refund result.
         */
        transition(
                payment,
                PaymentStatus.REFUND_PENDING
        );

        paymentRepository.saveAndFlush(
                payment
        );

        /*
         * Execute the refund.
         */
        executeRefund(
                payment,
                refund
        );

        return toResponse(refund);
    }

    @Transactional
    public PaymentRefundResponse retryRefund(
            UUID refundId
    ) {

        /*
         * Lock the refund row.
         */
        PaymentRefund refund =
                paymentRefundRepository
                        .findByIdForUpdate(refundId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Refund not found: "
                                                + refundId
                                )
                        );

        /*
         * A successful refund must never be retried.
         */
        if (refund.getStatus()
                == PaymentRefundStatus.SUCCEEDED) {

            return toResponse(refund);
        }

        /*
         * Load and lock the associated payment.
         */
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(
                                refund.getPaymentId()
                        )
                        .orElseThrow(
                                () -> new PaymentNotFoundException(
                                        refund.getPaymentId()
                                )
                        );

        /*
         * The payment must still represent a refund
         * that needs completion.
         */
        if (payment.getStatus()
                != PaymentStatus.REFUND_PENDING) {

            throw new IllegalStateException(
                    "Refund cannot be retried because payment "
                            + "is in status "
                            + payment.getStatus()
            );
        }

        /*
         * Move the refund back to PENDING for
         * another gateway attempt.
         */
        refund.setStatus(
                PaymentRefundStatus.PENDING
        );

        paymentRefundRepository.saveAndFlush(
                refund
        );

        /*
         * Execute the refund again.
         */
        executeRefund(
                payment,
                refund
        );

        return toResponse(refund);
    }

    private void executeRefund(
            Payment payment,
            PaymentRefund refund
    ) {

        PaymentRefundGatewayResult result =
                paymentRefundGateway.refund(
                        refund
                );

        if (result.successful()) {

            /*
             * Refund succeeded.
             *
             * Store the real gateway refund reference.
             */
            refund.setRefundReference(
                    result.refundReference()
            );

            refund.setStatus(
                    PaymentRefundStatus.SUCCEEDED
            );

            /*
             * The payment is now fully refunded.
             */
            transition(
                    payment,
                    PaymentStatus.REFUNDED
            );

            /*
             * IMPORTANT:
             *
             * Create PAYMENT_REFUNDED only after
             * the refund and payment state have
             * successfully transitioned.
             *
             * PaymentEventService stores this event
             * in the transactional outbox.
             */
            paymentEventService
                    .createPaymentRefundedEvent(
                            payment,
                            refund
                    );

        } else {

            /*
             * Refund failed.
             *
             * Payment remains REFUND_PENDING.
             *
             * The refund can be explicitly retried later.
             */
            refund.setStatus(
                    PaymentRefundStatus.FAILED
            );
        }

        /*
         * Persist refund state and payment state
         * in the same transaction.
         */
        paymentRefundRepository.saveAndFlush(
                refund
        );

        paymentRepository.saveAndFlush(
                payment
        );
    }

    private void transition(
            Payment payment,
            PaymentStatus newStatus
    ) {

        stateTransitionService.validateTransition(
                payment.getStatus(),
                newStatus
        );

        payment.setStatus(newStatus);
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key must not be blank"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }

    private String generateRefundReference() {

        return "REF-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private PaymentRefundResponse toResponse(
            PaymentRefund refund
    ) {

        return new PaymentRefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getOrderId(),
                refund.getRefundReference(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getIdempotencyKey(),
                refund.getVersion(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}
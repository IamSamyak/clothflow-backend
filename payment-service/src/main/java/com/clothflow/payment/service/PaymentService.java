package com.clothflow.payment.service;

import com.clothflow.payment.config.PaymentMetrics;
import com.clothflow.payment.dto.request.CreatePaymentRequest;
import com.clothflow.payment.dto.response.PaymentResponse;
import com.clothflow.payment.entity.Payment;
import com.clothflow.payment.entity.PaymentStatus;
import com.clothflow.payment.exception.IdempotencyConflictException;
import com.clothflow.payment.exception.PaymentNotFoundException;
import com.clothflow.payment.gateway.PaymentGateway;
import com.clothflow.payment.gateway.PaymentGatewayResult;
import com.clothflow.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentGateway paymentGateway;

    private final PaymentStateTransitionService
            stateTransitionService;

    private final PaymentEventService paymentEventService;

    private final PaymentMetrics paymentMetrics;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentStateTransitionService stateTransitionService,
            PaymentEventService paymentEventService,
            PaymentMetrics paymentMetrics
    ) {
        this.paymentRepository =
                paymentRepository;

        this.paymentGateway =
                paymentGateway;

        this.stateTransitionService =
                stateTransitionService;

        this.paymentEventService =
                paymentEventService;

        this.paymentMetrics =
                paymentMetrics;
    }

    @Transactional
    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            String idempotencyKey
    ) {

        validateIdempotencyKey(idempotencyKey);

        /*
         * First resolve idempotency.
         *
         * An idempotent retry that returns an existing payment
         * is NOT a new payment-processing attempt.
         */
        Payment existingPayment =
                paymentRepository
                        .findByIdempotencyKeyForUpdate(
                                idempotencyKey
                        )
                        .orElse(null);

        if (existingPayment != null) {

            if (!existingPayment.matchesRequest(
                    request.orderId(),
                    request.customerId(),
                    request.amount(),
                    "INR",
                    request.paymentMethod()
            )) {

                throw new IdempotencyConflictException(
                        idempotencyKey
                );
            }

            return toResponse(existingPayment);
        }

        /*
         * From this point onward we know this is a genuinely
         * new payment-processing attempt.
         */
        Timer.Sample processingTimer =
                paymentMetrics.startProcessingTimer();

        try {

            /*
             * Generate a unique internal payment reference.
             */
            String paymentReference =
                    generatePaymentReference();

            Payment payment =
                    new Payment(
                            paymentReference,
                            idempotencyKey,
                            request.orderId(),
                            request.customerId(),
                            request.amount(),
                            "INR",
                            request.paymentMethod()
                    );

            paymentRepository.saveAndFlush(payment);

            /*
             * A genuine payment record has been created.
             */
            paymentMetrics.paymentCreated();

            /*
             * Initial state:
             *
             * PENDING
             */
            transition(
                    payment,
                    PaymentStatus.PENDING
            );

            /*
             * External gateway call.
             *
             * This is the critical external boundary.
             *
             * Possible outcomes:
             *
             * 1. Successful result
             * 2. Definitive failure result
             * 3. Exception / ambiguous outcome
             */
            PaymentGatewayResult result =
                    paymentGateway.process(payment);

            /*
             * Definitive gateway success.
             */
            if (result.successful()) {

                transition(
                        payment,
                        PaymentStatus.SUCCEEDED
                );

                /*
                 * Persist the successful payment event
                 * through the existing Outbox mechanism.
                 */
                paymentEventService
                        .createPaymentSucceededEvent(
                                payment
                        );

                /*
                 * Count only a genuine successful payment.
                 */
                paymentMetrics.paymentSucceeded();

            } else {

                /*
                 * Definitive gateway failure.
                 */
                transition(
                        payment,
                        PaymentStatus.FAILED
                );

                /*
                 * Count only a genuine failed payment.
                 */
                paymentMetrics.paymentFailed();
            }

            /*
             * Persist the final payment state.
             */
            paymentRepository.saveAndFlush(payment);

            return toResponse(payment);

        } finally {

            /*
             * IMPORTANT:
             *
             * Always record processing latency.
             *
             * This executes for:
             *
             * - SUCCESS
             * - definitive FAILURE
             * - gateway exception
             * - database exception
             * - any other runtime exception
             *
             * Therefore the latency metric represents the actual
             * duration of payment-processing attempts rather than
             * only successful requests.
             */
            paymentMetrics.recordProcessing(
                    processingTimer
            );
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID paymentId
    ) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(
                                () ->
                                        new PaymentNotFoundException(
                                                paymentId
                                        )
                        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID paymentId,
            UUID customerId
    ) {

        Payment payment =
                paymentRepository
                        .findByIdAndCustomerId(
                                paymentId,
                                customerId
                        )
                        .orElseThrow(
                                () ->
                                        new PaymentNotFoundException(
                                                paymentId
                                        )
                        );

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(
            UUID orderId
    ) {

        return paymentRepository
                .findAllByOrderId(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(
            UUID orderId,
            UUID customerId
    ) {

        return paymentRepository
                .findAllByOrderIdAndCustomerId(
                        orderId,
                        customerId
                )
                .stream()
                .map(this::toResponse)
                .toList();
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

    private String generatePaymentReference() {

        return "PAY-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    private PaymentResponse toResponse(
            Payment payment
    ) {

        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getIdempotencyKey(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getVersion(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
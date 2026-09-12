package com.clothflow.payment.controller;

import com.clothflow.payment.dto.request.CreatePaymentRequest;
import com.clothflow.payment.dto.response.PaymentRefundResponse;
import com.clothflow.payment.dto.response.PaymentResponse;
import com.clothflow.payment.security.CurrentUserService;
import com.clothflow.payment.service.PaymentRefundService;
import com.clothflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    private final PaymentRefundService paymentRefundService;

    private final CurrentUserService currentUserService;

    public PaymentController(
            PaymentService paymentService,
            PaymentRefundService paymentRefundService,
            CurrentUserService currentUserService
    ) {
        this.paymentService =
                paymentService;

        this.paymentRefundService =
                paymentRefundService;

        this.currentUserService =
                currentUserService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_payment.write')"
    )
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            CreatePaymentRequest request
    ) {

        return ResponseEntity.ok(
                paymentService.createPayment(
                        request,
                        idempotencyKey
                )
        );
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(
                        authentication
                );

        return ResponseEntity.ok(
                paymentService.getPayment(
                        paymentId,
                        customerId
                )
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(
                        authentication
                );

        return ResponseEntity.ok(
                paymentService.getPaymentsByOrderId(
                        orderId,
                        customerId
                )
        );
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_payment.write')"
    )
    public ResponseEntity<PaymentRefundResponse> refundPayment(
            @PathVariable UUID paymentId,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey
    ) {

        return ResponseEntity.ok(
                paymentRefundService.refundPayment(
                        paymentId,
                        idempotencyKey
                )
        );
    }

    @PostMapping("/refunds/{refundId}/retry")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_payment.write')"
    )
    public ResponseEntity<PaymentRefundResponse> retryRefund(
            @PathVariable UUID refundId
    ) {

        return ResponseEntity.ok(
                paymentRefundService.retryRefund(
                        refundId
                )
        );
    }
}
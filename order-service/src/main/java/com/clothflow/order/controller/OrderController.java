package com.clothflow.order.controller;

import com.clothflow.order.dto.request.CancelOrderRequest;
import com.clothflow.order.dto.request.CreateOrderRequest;
import com.clothflow.order.dto.request.UpdateOrderStatusRequest;
import com.clothflow.order.dto.response.OrderResponse;
import com.clothflow.order.security.CurrentUserService;
import com.clothflow.order.service.OrderService;
import com.clothflow.order.service.PaymentTransitionResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(
            OrderService orderService,
            CurrentUserService currentUserService
    ) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    /**
     * Customer creates an order.
     *
     * customerId is intentionally NOT accepted from the request body.
     * It comes from JWT.sub.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(authentication);

        OrderResponse response =
                orderService.createOrder(
                        request,
                        customerId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Internal order state transition.
     *
     * This endpoint must NOT be callable by a normal customer JWT.
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        OrderResponse response =
                orderService.updateStatus(
                        orderId,
                        request.status(),
                        request.reason()
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Customer can retrieve only their own order.
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(authentication);

        return ResponseEntity.ok(
                orderService.getOrder(
                        orderId,
                        customerId
                )
        );
    }

    /**
     * Customer can cancel only their own order.
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @RequestBody(required = false)
            CancelOrderRequest request,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(authentication);

        String reason =
                request != null
                        ? request.reason()
                        : null;

        return ResponseEntity.ok(
                orderService.cancelOrder(
                        orderId,
                        customerId,
                        reason
                )
        );
    }

    /**
     * Internal payment workflow transition.
     */
    @PostMapping("/{orderId}/payment-pending")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_payment.write')"
    )
    public ResponseEntity<PaymentTransitionResult> markPaymentPending(
            @PathVariable UUID orderId
    ) {

        return ResponseEntity.ok(
                orderService.markPaymentPending(orderId)
        );
    }

    /**
     * Customer can initiate payment only for their own order.
     */
    @PostMapping("/{orderId}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> payOrder(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(authentication);

        return ResponseEntity.ok(
                orderService.processPayment(
                        orderId,
                        customerId
                )
        );
    }
}
package com.clothflow.shipping.controller;

import com.clothflow.shipping.dto.request.CreateShipmentRequest;
import com.clothflow.shipping.dto.response.ShipmentResponse;
import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.mapper.ShipmentMapper;
import com.clothflow.shipping.provider.CarrierTrackingWebhookRequest;
import com.clothflow.shipping.security.CurrentUserService;
import com.clothflow.shipping.service.ShipmentService;
import com.clothflow.shipping.service.ShipmentTrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    private final ShipmentMapper shipmentMapper;

    private final ShipmentTrackingService shipmentTrackingService;

    private final CurrentUserService currentUserService;

    public ShipmentController(
            ShipmentService shipmentService,
            ShipmentMapper shipmentMapper,
            ShipmentTrackingService shipmentTrackingService,
            CurrentUserService currentUserService
    ) {
        this.shipmentService = shipmentService;
        this.shipmentMapper = shipmentMapper;
        this.shipmentTrackingService = shipmentTrackingService;
        this.currentUserService = currentUserService;
    }

    /*
     * ============================================================
     * CREATE SHIPMENT
     * ============================================================
     *
     * Internal Shipping operation.
     *
     * Caller:
     *     Service JWT
     *
     * Required:
     *     TOKEN_SERVICE
     *     SCOPE_order.shipping.write
     */
    @PostMapping
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid
            @RequestBody
            CreateShipmentRequest request
    ) {

        Shipment shipment =
                shipmentService.createShipment(
                        request.orderId(),
                        request.customerId(),
                        request.recipientName(),
                        request.addressLine1(),
                        request.addressLine2(),
                        request.city(),
                        request.state(),
                        request.postalCode(),
                        request.country(),
                        request.items()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        shipmentMapper.toResponse(
                                shipment
                        )
                );
    }

    /*
     * ============================================================
     * GET SHIPMENT
     * ============================================================
     *
     * Customer operation.
     *
     * The customer can only retrieve a shipment that belongs
     * to the authenticated customer.
     */
    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShipmentResponse> getShipment(
            @PathVariable UUID shipmentId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(
                        authentication
                );

        Shipment shipment =
                shipmentService.getShipment(
                        shipmentId,
                        customerId
                );

        return ResponseEntity.ok(
                shipmentMapper.toResponse(
                        shipment
                )
        );
    }

    /*
     * ============================================================
     * GET SHIPMENT BY ORDER
     * ============================================================
     *
     * Customer operation.
     *
     * The customer can only retrieve the shipment for an order
     * that belongs to the authenticated customer.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShipmentResponse> getShipmentByOrderId(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {

        UUID customerId =
                currentUserService.getCurrentUserId(
                        authentication
                );

        Shipment shipment =
                shipmentService.getShipmentByOrderId(
                        orderId,
                        customerId
                );

        return ResponseEntity.ok(
                shipmentMapper.toResponse(
                        shipment
                )
        );
    }

    /*
     * ============================================================
     * PENDING → PROCESSING
     * ============================================================
     *
     * Internal Shipping operation.
     */
    @PostMapping("/{shipmentId}/processing")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> startProcessing(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.startProcessing(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * SHIPPED → OUT_FOR_DELIVERY
     * ============================================================
     *
     * Internal Shipping operation.
     */
    @PostMapping("/{shipmentId}/out-for-delivery")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> markOutForDelivery(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.markOutForDelivery(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * OUT_FOR_DELIVERY → DELIVERED
     * ============================================================
     *
     * Internal Shipping operation.
     */
    @PostMapping("/{shipmentId}/delivered")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> markDelivered(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.markDelivered(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * DELIVERY FAILURE
     * ============================================================
     *
     * Internal Shipping operation.
     */
    @PostMapping("/{shipmentId}/delivery-failed")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> markDeliveryFailed(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.markDeliveryFailed(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * DELIVERY_FAILED → PROCESSING
     * ============================================================
     *
     * Internal retry operation.
     */
    @PostMapping("/{shipmentId}/retry")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> retryDelivery(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.retryDelivery(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * CANCEL
     * ============================================================
     *
     * Internal Shipping operation.
     */
    @PostMapping("/{shipmentId}/cancel")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> cancel(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.cancel(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * SHIP USING PROVIDER
     * ============================================================
     *
     * Internal Shipping/provider operation.
     */
    @PostMapping("/{shipmentId}/ship")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') " +
                    "and hasAuthority('SCOPE_order.shipping.write')"
    )
    public ResponseEntity<ShipmentResponse> ship(
            @PathVariable UUID shipmentId
    ) {

        return ResponseEntity.ok(
                shipmentService.shipUsingProvider(
                        shipmentId
                )
        );
    }

    /*
     * ============================================================
     * CARRIER TRACKING WEBHOOK
     * ============================================================
     *
     * This endpoint intentionally does NOT use JWT authentication.
     *
     * The external carrier authenticates itself through:
     *
     *     X-Provider-Event-ID
     *     X-Provider-Timestamp
     *     X-Provider-Signature
     *
     * Signature verification and webhook idempotency are handled
     * inside ShipmentTrackingService.
     */
    @PostMapping("/webhooks/tracking")
    public ResponseEntity<Void> trackingWebhook(
            @RequestHeader("X-Provider-Event-ID")
            String providerEventId,

            @RequestHeader("X-Provider-Timestamp")
            String providerTimestamp,

            @RequestHeader("X-Provider-Signature")
            String providerSignature,

            @Valid
            @RequestBody
            CarrierTrackingWebhookRequest request
    ) {

        shipmentTrackingService.handleTrackingWebhook(
                providerEventId,
                request,
                providerTimestamp,
                providerSignature
        );

        return ResponseEntity
                .accepted()
                .build();
    }
}
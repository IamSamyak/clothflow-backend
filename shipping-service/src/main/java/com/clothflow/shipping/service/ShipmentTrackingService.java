package com.clothflow.shipping.service;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import com.clothflow.shipping.provider.CarrierTrackingWebhookRequest;
import com.clothflow.shipping.provider.ShippingProvider;
import com.clothflow.shipping.provider.TrackingInfo;
import com.clothflow.shipping.provider.WebhookSignatureVerifier;
import com.clothflow.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ShipmentTrackingService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShipmentTrackingService.class
            );

    private final ShipmentRepository shipmentRepository;

    private final ShippingProvider shippingProvider;

    private final ShipmentWebhookPersistenceService
            webhookPersistenceService;

    private final WebhookSignatureVerifier
            webhookSignatureVerifier;

    private final ShipmentTrackingPersistenceService
            trackingPersistenceService;

    public ShipmentTrackingService(
            ShipmentRepository shipmentRepository,
            ShippingProvider shippingProvider,
            ShipmentWebhookPersistenceService webhookPersistenceService,
            WebhookSignatureVerifier webhookSignatureVerifier,
            ShipmentTrackingPersistenceService trackingPersistenceService
    ) {

        this.shipmentRepository =
                shipmentRepository;

        this.shippingProvider =
                shippingProvider;

        this.webhookPersistenceService =
                webhookPersistenceService;

        this.webhookSignatureVerifier =
                webhookSignatureVerifier;

        this.trackingPersistenceService =
                trackingPersistenceService;
    }

    /*
     * ============================================================
     * PROVIDER TRACKING SYNCHRONIZATION
     * ============================================================
     *
     * Called when Shipping actively synchronizes shipment tracking
     * with the external carrier.
     *
     * Important:
     *
     * The external provider call happens outside the database
     * transaction.
     *
     * Only the resulting state update enters the transactional
     * persistence boundary.
     */
    public void synchronizeShipment(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Shipment not found: "
                                        + shipmentId
                        )
                );

        /*
         * No tracking number means there is nothing to synchronize.
         */
        if (shipment.getTrackingNumber() == null
                || shipment.getTrackingNumber().isBlank()) {

            log.debug(
                    "Skipping tracking synchronization " +
                            "because shipment has no tracking number: {}",
                    shipmentId
            );

            return;
        }

        /*
         * Terminal shipments do not need further synchronization.
         */
        if (shipment.getStatus()
                == ShipmentStatus.DELIVERED
                ||
                shipment.getStatus()
                        == ShipmentStatus.CANCELLED) {

            return;
        }

        /*
         * ========================================================
         * EXTERNAL PROVIDER CALL
         * ========================================================
         *
         * Do NOT hold a database transaction while calling the
         * external provider.
         */
        TrackingInfo trackingInfo =
                shippingProvider.getTrackingInfo(
                        shipment.getTrackingNumber()
                );

        /*
         * ========================================================
         * TRANSACTIONAL DATABASE UPDATE
         * ========================================================
         *
         * Persistence service owns the transaction and optimistic
         * concurrency handling.
         */
        trackingPersistenceService.applyProviderUpdate(
                shipmentId,
                trackingInfo
        );
    }

    /*
     * ============================================================
     * CARRIER TRACKING WEBHOOK
     * ============================================================
     *
     * External carrier → Shipping Service.
     *
     * Authentication model:
     *
     *     1. Event ID consistency
     *     2. HMAC/signature verification
     *     3. Provider state verification
     *     4. Idempotent persistence
     *
     * No customer JWT is involved here.
     */
    public void handleTrackingWebhook(
            String providerEventId,
            CarrierTrackingWebhookRequest request,
            String providerTimestamp,
            String providerSignature
    ) {

        /*
         * ========================================================
         * 1. VALIDATE EVENT-ID CONSISTENCY
         * ========================================================
         *
         * The event ID exists in both:
         *
         *     X-Provider-Event-ID
         *
         * and
         *
         *     request.providerEventId()
         *
         * They must represent the same event.
         *
         * This prevents an attacker/integration bug from mixing
         * headers and payloads.
         */
        if (providerEventId == null
                || providerEventId.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing shipping provider event ID"
            );
        }

        if (request.providerEventId() == null
                || request.providerEventId().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing provider event ID in webhook payload"
            );
        }

        if (!providerEventId.equals(
                request.providerEventId()
        )) {

            throw new IllegalArgumentException(
                    "Provider event ID header does not match " +
                            "webhook payload"
            );
        }

        /*
         * ========================================================
         * 2. VERIFY SIGNATURE
         * ========================================================
         *
         * The provider event ID is part of the signed payload.
         *
         * Signature verification MUST happen before any database
         * mutation.
         */
        boolean validSignature =
                webhookSignatureVerifier.verify(
                        request.providerEventId(),
                        providerTimestamp,
                        request.trackingNumber(),
                        request.status().name(),
                        providerSignature
                );

        if (!validSignature) {

            throw new IllegalArgumentException(
                    "Invalid shipping provider webhook signature"
            );
        }

        /*
         * ========================================================
         * 3. VERIFY PROVIDER STATE
         * ========================================================
         *
         * Do not blindly trust the webhook body.
         *
         * Ask the provider for the current tracking state and
         * ensure it agrees with the webhook.
         */
        TrackingInfo trackingInfo =
                shippingProvider.getTrackingInfo(
                        request.trackingNumber()
                );

        if (trackingInfo.status()
                != request.status()) {

            throw new IllegalArgumentException(
                    "Webhook status does not match provider status"
            );
        }

        /*
         * ========================================================
         * 4. IDEMPOTENT PERSISTENCE
         * ========================================================
         *
         * ShipmentWebhookPersistenceService owns the database
         * transaction and duplicate-event protection.
         *
         * providerEventId ultimately becomes the idempotency key
         * through request.providerEventId().
         */
        boolean newlyProcessed =
                webhookPersistenceService.processWebhook(
                        request,
                        trackingInfo
                );

        /*
         * Duplicate events are intentionally treated as successful
         * processing from the integration perspective.
         *
         * This is important because carriers commonly retry
         * webhook delivery when they do not receive a response
         * quickly enough.
         */
        if (!newlyProcessed) {

            log.info(
                    "Duplicate carrier webhook ignored: " +
                            "providerEventId={}, trackingNumber={}",
                    request.providerEventId(),
                    request.trackingNumber()
            );
        }
    }
}

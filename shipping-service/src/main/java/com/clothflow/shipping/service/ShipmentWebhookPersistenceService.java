package com.clothflow.shipping.service;

import com.clothflow.shipping.provider.CarrierTrackingWebhookRequest;
import com.clothflow.shipping.provider.TrackingInfo;
import com.clothflow.shipping.repository.ProcessedWebhookRepository;
import com.clothflow.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentWebhookPersistenceService {

    private final ProcessedWebhookRepository
            processedWebhookRepository;

    private final ShipmentRepository shipmentRepository;

    private final ShipmentTrackingPersistenceService
            trackingPersistenceService;

    public ShipmentWebhookPersistenceService(
            ProcessedWebhookRepository
                    processedWebhookRepository,
            ShipmentRepository shipmentRepository,
            ShipmentTrackingPersistenceService
                    trackingPersistenceService
    ) {
        this.processedWebhookRepository =
                processedWebhookRepository;

        this.shipmentRepository =
                shipmentRepository;

        this.trackingPersistenceService =
                trackingPersistenceService;
    }

    @Transactional
    public boolean processWebhook(
            CarrierTrackingWebhookRequest request,
            TrackingInfo trackingInfo
    ) {

        int inserted =
                processedWebhookRepository
                        .insertIfNotProcessed(
                                request.providerEventId(),
                                "FAKE_PROVIDER",
                                request.status().name(),
                                request.trackingNumber()
                        );

        if (inserted == 0) {
            return false;
        }

        var shipment =
                shipmentRepository
                        .findByTrackingNumber(
                                request.trackingNumber()
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Shipment not found for tracking number: "
                                                + request.trackingNumber()
                                )
                        );

        trackingPersistenceService
                .applyProviderUpdate(
                        shipment.getId(),
                        trackingInfo
                );

        return true;
    }
}
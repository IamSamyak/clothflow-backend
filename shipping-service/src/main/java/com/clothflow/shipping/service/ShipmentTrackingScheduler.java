package com.clothflow.shipping.service;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import com.clothflow.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShipmentTrackingScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShipmentTrackingScheduler.class
            );

    private final ShipmentRepository shipmentRepository;

    private final ShipmentTrackingService trackingService;

    public ShipmentTrackingScheduler(
            ShipmentRepository shipmentRepository,
            ShipmentTrackingService trackingService
    ) {
        this.shipmentRepository =
                shipmentRepository;

        this.trackingService =
                trackingService;
    }

    @Scheduled(fixedDelay = 60000)
    public void synchronizeActiveShipments() {

        shipmentRepository
                .findByStatusIn(
                        java.util.List.of(
                                ShipmentStatus.SHIPPED,
                                ShipmentStatus.OUT_FOR_DELIVERY
                        )
                )
                .forEach(
                        shipment -> synchronize(
                                shipment
                        )
                );
    }

    private void synchronize(
            Shipment shipment
    ) {

        try {

            trackingService.synchronizeShipment(
                    shipment.getId()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to synchronize shipment tracking: " +
                            "shipmentId={}",
                    shipment.getId(),
                    ex
            );
        }
    }
}
package com.clothflow.shipping.service;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import com.clothflow.shipping.event.ShippingEventType;
import com.clothflow.shipping.outbox.ShippingOutboxService;
import com.clothflow.shipping.provider.TrackingInfo;
import com.clothflow.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShipmentTrackingPersistenceService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShipmentTrackingPersistenceService.class
            );

    private final ShipmentRepository shipmentRepository;

    private final ShippingOutboxService shippingOutboxService;

    public ShipmentTrackingPersistenceService(
            ShipmentRepository shipmentRepository,
            ShippingOutboxService shippingOutboxService
    ) {
        this.shipmentRepository =
                shipmentRepository;

        this.shippingOutboxService =
                shippingOutboxService;
    }

    @Transactional
    public void applyProviderUpdate(
            UUID shipmentId,
            TrackingInfo trackingInfo
    ) {

        Shipment shipment =
                shipmentRepository
                        .findByIdForUpdate(
                                shipmentId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Shipment not found: "
                                                + shipmentId
                                )
                        );

        if (shipment.getStatus()
                == ShipmentStatus.DELIVERED
                ||
                shipment.getStatus()
                        == ShipmentStatus.CANCELLED) {

            return;
        }

        synchronizeStatus(
                shipment,
                trackingInfo
        );
    }

    private void synchronizeStatus(
            Shipment shipment,
            TrackingInfo trackingInfo
    ) {

        ShipmentStatus providerStatus =
                trackingInfo.status();

        if (providerStatus == null) {
            return;
        }

        if (providerStatus ==
                shipment.getStatus()) {

            return;
        }

        log.info(
                "Synchronizing shipment status: " +
                        "shipmentId={}, localStatus={}, " +
                        "providerStatus={}",
                shipment.getId(),
                shipment.getStatus(),
                providerStatus
        );

        switch (providerStatus) {

            case OUT_FOR_DELIVERY -> {

                if (shipment.getStatus()
                        == ShipmentStatus.SHIPPED) {

                    shipment.markOutForDelivery();

                    persistLifecycleEvent(
                            shipment,
                            ShippingEventType
                                    .SHIPMENT_OUT_FOR_DELIVERY
                    );
                }
            }

            case DELIVERED -> {

                /*
                 * Provider can jump directly from
                 * SHIPPED to DELIVERED.
                 *
                 * Our domain requires:
                 *
                 * SHIPPED
                 *     ↓
                 * OUT_FOR_DELIVERY
                 *     ↓
                 * DELIVERED
                 */

                if (shipment.getStatus()
                        == ShipmentStatus.SHIPPED) {

                    shipment.markOutForDelivery();

                    persistLifecycleEvent(
                            shipment,
                            ShippingEventType
                                    .SHIPMENT_OUT_FOR_DELIVERY
                    );
                }

                if (shipment.getStatus()
                        == ShipmentStatus.OUT_FOR_DELIVERY) {

                    shipment.markDelivered();

                    persistLifecycleEvent(
                            shipment,
                            ShippingEventType
                                    .SHIPMENT_DELIVERED
                    );
                }
            }

            default -> {

                log.debug(
                        "No local transition mapping for " +
                                "provider status: {}",
                        providerStatus
                );
            }
        }
    }

    private void persistLifecycleEvent(
            Shipment shipment,
            ShippingEventType eventType
    ) {

        shipmentRepository.save(
                shipment
        );

        shippingOutboxService
                .createShipmentLifecycleEvent(
                        shipment,
                        eventType.name()
                );
    }
}
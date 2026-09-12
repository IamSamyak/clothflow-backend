package com.clothflow.shipping.provider;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakeShippingProvider
        implements ShippingProvider {

    private final Map<String, ShippingLabel> createdShipments =
            new ConcurrentHashMap<>();

    private final Map<String, ShipmentStatus> trackingStatuses =
            new ConcurrentHashMap<>();

    @Override
    public ShippingLabel createShipment(
            Shipment shipment,
            String idempotencyKey
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency key is required"
            );
        }

        return createdShipments.computeIfAbsent(
                idempotencyKey,
                key -> {

                    String trackingNumber =
                            "CF-"
                                    + UUID.randomUUID()
                                    .toString()
                                    .substring(0, 8)
                                    .toUpperCase();

                    trackingStatuses.put(
                            trackingNumber,
                            ShipmentStatus.SHIPPED
                    );

                    return new ShippingLabel(
                            "CLOTHFLOW-FAKE",
                            trackingNumber
                    );
                }
        );
    }

    @Override
    public TrackingInfo getTrackingInfo(
            String trackingNumber
    ) {

        ShipmentStatus status =
                trackingStatuses.get(
                        trackingNumber
                );

        if (status == null) {

            throw new ShippingProviderException(
                    "Tracking number not found: "
                            + trackingNumber
            );
        }

        return new TrackingInfo(
                trackingNumber,
                status
        );
    }

    public void updateTrackingStatus(
            String trackingNumber,
            ShipmentStatus status
    ) {

        if (!trackingStatuses.containsKey(
                trackingNumber
        )) {

            throw new IllegalArgumentException(
                    "Tracking number not found: "
                            + trackingNumber
            );
        }

        trackingStatuses.put(
                trackingNumber,
                status
        );
    }
}
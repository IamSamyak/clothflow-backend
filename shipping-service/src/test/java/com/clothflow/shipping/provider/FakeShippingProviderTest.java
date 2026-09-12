package com.clothflow.shipping.provider;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FakeShippingProviderTest {

    @Test
    void shouldReturnSameShipmentForSameIdempotencyKey() {

        FakeShippingProvider provider =
                new FakeShippingProvider();

        Shipment shipment =
                new Shipment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Samyak",
                        "123 Main Street",
                        null,
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "India"
                );

        String idempotencyKey =
                "clothflow-shipment-test";

        ShippingLabel first =
                provider.createShipment(
                        shipment,
                        idempotencyKey
                );

        ShippingLabel second =
                provider.createShipment(
                        shipment,
                        idempotencyKey
                );

        assertEquals(
                first,
                second
        );
    }

    @Test
    void shouldCreateDifferentShipmentForDifferentIdempotencyKeys() {

        FakeShippingProvider provider =
                new FakeShippingProvider();

        Shipment shipment =
                new Shipment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Samyak",
                        "123 Main Street",
                        null,
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "India"
                );

        ShippingLabel first =
                provider.createShipment(
                        shipment,
                        "key-1"
                );

        ShippingLabel second =
                provider.createShipment(
                        shipment,
                        "key-2"
                );

        assertNotEquals(
                first.trackingNumber(),
                second.trackingNumber()
        );
    }

    @Test
    void shouldInitiallySetTrackingStatusToShipped() {

        FakeShippingProvider provider =
                new FakeShippingProvider();

        Shipment shipment =
                new Shipment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Samyak",
                        "123 Main Street",
                        null,
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "India"
                );

        ShippingLabel label =
                provider.createShipment(
                        shipment,
                        "key-1"
                );

        TrackingInfo trackingInfo =
                provider.getTrackingInfo(
                        label.trackingNumber()
                );

        assertEquals(
                ShipmentStatus.SHIPPED,
                trackingInfo.status()
        );
    }

    @Test
    void shouldAllowTrackingStatusUpdate() {

        FakeShippingProvider provider =
                new FakeShippingProvider();

        Shipment shipment =
                new Shipment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Samyak",
                        "123 Main Street",
                        null,
                        "Pune",
                        "Maharashtra",
                        "411001",
                        "India"
                );

        ShippingLabel label =
                provider.createShipment(
                        shipment,
                        "key-1"
                );

        provider.updateTrackingStatus(
                label.trackingNumber(),
                ShipmentStatus.OUT_FOR_DELIVERY
        );

        TrackingInfo trackingInfo =
                provider.getTrackingInfo(
                        label.trackingNumber()
                );

        assertEquals(
                ShipmentStatus.OUT_FOR_DELIVERY,
                trackingInfo.status()
        );
    }
}
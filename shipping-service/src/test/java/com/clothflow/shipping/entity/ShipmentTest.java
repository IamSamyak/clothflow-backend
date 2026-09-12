package com.clothflow.shipping.entity;

import com.clothflow.shipping.exception.InvalidShipmentStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    private Shipment createShipment() {

        return new Shipment(
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
    }

    @Test
    void shouldMoveFromPendingToProcessing() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        assertEquals(
                ShipmentStatus.PROCESSING,
                shipment.getStatus()
        );
    }

    @Test
    void shouldMoveFromProcessingToShipped() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        assertEquals(
                ShipmentStatus.SHIPPED,
                shipment.getStatus()
        );

        assertEquals(
                "CLOTHFLOW-FAKE",
                shipment.getCarrier()
        );

        assertEquals(
                "CF-ABC12345",
                shipment.getTrackingNumber()
        );

        assertNotNull(
                shipment.getShippedAt()
        );
    }

    @Test
    void shouldMoveFromShippedToOutForDelivery() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        shipment.markOutForDelivery();

        assertEquals(
                ShipmentStatus.OUT_FOR_DELIVERY,
                shipment.getStatus()
        );
    }

    @Test
    void shouldMoveFromOutForDeliveryToDelivered() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        shipment.markOutForDelivery();

        shipment.markDelivered();

        assertEquals(
                ShipmentStatus.DELIVERED,
                shipment.getStatus()
        );

        assertNotNull(
                shipment.getDeliveredAt()
        );
    }

    @Test
    void shouldAllowDeliveryFailureFromShipped() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        shipment.markDeliveryFailed();

        assertEquals(
                ShipmentStatus.DELIVERY_FAILED,
                shipment.getStatus()
        );
    }

    @Test
    void shouldRetryFailedDelivery() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        shipment.markDeliveryFailed();

        shipment.retryDelivery();

        assertEquals(
                ShipmentStatus.PROCESSING,
                shipment.getStatus()
        );
    }

    @Test
    void shouldNotAllowDeliveryBeforeShipping() {

        Shipment shipment = createShipment();

        assertThrows(
                InvalidShipmentStatusTransitionException.class,
                shipment::markDelivered
        );
    }

    @Test
    void shouldNotAllowShippingWithoutCarrier() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        assertThrows(
                IllegalArgumentException.class,
                () -> shipment.markShipped(
                        null,
                        "CF-ABC12345"
                )
        );
    }

    @Test
    void shouldNotAllowShippingWithoutTrackingNumber() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        assertThrows(
                IllegalArgumentException.class,
                () -> shipment.markShipped(
                        "CLOTHFLOW-FAKE",
                        null
                )
        );
    }

    @Test
    void shouldNotAllowModificationAfterDelivered() {

        Shipment shipment = createShipment();

        shipment.startProcessing();

        shipment.markShipped(
                "CLOTHFLOW-FAKE",
                "CF-ABC12345"
        );

        shipment.markOutForDelivery();

        shipment.markDelivered();

        assertThrows(
                InvalidShipmentStatusTransitionException.class,
                shipment::markOutForDelivery
        );
    }
}
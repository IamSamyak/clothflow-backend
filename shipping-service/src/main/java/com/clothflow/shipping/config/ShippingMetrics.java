package com.clothflow.shipping.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ShippingMetrics {

    private final Counter shipmentsCreated;
    private final Counter shipmentsShipped;
    private final Counter shipmentsOutForDelivery;
    private final Counter shipmentsDelivered;
    private final Counter shipmentsDeliveryFailed;
    private final Counter shipmentsCancelled;
    private final Counter providerFailures;

    private final Timer providerProcessingTimer;

    public ShippingMetrics(
            MeterRegistry meterRegistry
    ) {

        this.shipmentsCreated =
                Counter.builder("clothflow.shipping.shipment.created")
                        .description(
                                "Number of shipments created"
                        )
                        .register(meterRegistry);

        this.shipmentsShipped =
                Counter.builder("clothflow.shipping.shipment.shipped")
                        .description(
                                "Number of shipments marked as shipped"
                        )
                        .register(meterRegistry);

        this.shipmentsOutForDelivery =
                Counter.builder(
                                "clothflow.shipping.shipment.out_for_delivery"
                        )
                        .description(
                                "Number of shipments marked out for delivery"
                        )
                        .register(meterRegistry);

        this.shipmentsDelivered =
                Counter.builder(
                                "clothflow.shipping.shipment.delivered"
                        )
                        .description(
                                "Number of shipments successfully delivered"
                        )
                        .register(meterRegistry);

        this.shipmentsDeliveryFailed =
                Counter.builder(
                                "clothflow.shipping.shipment.delivery_failed"
                        )
                        .description(
                                "Number of shipments whose delivery failed"
                        )
                        .register(meterRegistry);

        this.shipmentsCancelled =
                Counter.builder(
                                "clothflow.shipping.shipment.cancelled"
                        )
                        .description(
                                "Number of shipments cancelled"
                        )
                        .register(meterRegistry);

        this.providerFailures =
                Counter.builder(
                                "clothflow.shipping.provider.failure"
                        )
                        .description(
                                "Number of shipping provider failures"
                        )
                        .register(meterRegistry);

        this.providerProcessingTimer =
                Timer.builder(
                                "clothflow.shipping.provider.processing"
                        )
                        .description(
                                "Time taken to create a shipment with the external shipping provider"
                        )
                        .publishPercentileHistogram()
                        .register(meterRegistry);
    }

    public void shipmentCreated() {
        shipmentsCreated.increment();
    }

    public void shipmentShipped() {
        shipmentsShipped.increment();
    }

    public void shipmentOutForDelivery() {
        shipmentsOutForDelivery.increment();
    }

    public void shipmentDelivered() {
        shipmentsDelivered.increment();
    }

    public void shipmentDeliveryFailed() {
        shipmentsDeliveryFailed.increment();
    }

    public void shipmentCancelled() {
        shipmentsCancelled.increment();
    }

    public void providerFailure() {
        providerFailures.increment();
    }

    public Timer.Sample startProviderProcessingTimer() {
        return Timer.start();
    }

    public void recordProviderProcessing(
            Timer.Sample sample
    ) {
        sample.stop(providerProcessingTimer);
    }
}
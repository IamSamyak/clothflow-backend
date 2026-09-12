package com.clothflow.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter ordersCreated;

    private final Counter inventoryReserved;

    private final Counter inventoryFailed;

    private final Counter paymentSucceeded;

    private final Counter paymentFailed;

    private final Counter ordersConfirmed;

    private final Counter ordersCancelled;

    private final Timer orderProcessingTimer;

    public OrderMetrics(MeterRegistry meterRegistry) {

        this.ordersCreated = Counter.builder("clothflow.order.created")
                .description("Number of orders created")
                .register(meterRegistry);

        this.inventoryReserved = Counter.builder("clothflow.order.inventory.reserved")
                .description("Number of orders for which inventory was reserved")
                .register(meterRegistry);

        this.inventoryFailed = Counter.builder("clothflow.order.inventory.failed")
                .description("Number of orders where inventory reservation failed")
                .register(meterRegistry);

        this.paymentSucceeded = Counter.builder("clothflow.order.payment.succeeded")
                .description("Number of orders with successful payment")
                .register(meterRegistry);

        this.paymentFailed = Counter.builder("clothflow.order.payment.failed")
                .description("Number of orders with failed payment")
                .register(meterRegistry);

        this.ordersConfirmed = Counter.builder("clothflow.order.confirmed")
                .description("Number of confirmed orders")
                .register(meterRegistry);

        this.ordersCancelled = Counter.builder("clothflow.order.cancelled")
                .description("Number of cancelled orders")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("clothflow.order.processing")
                .description("Time taken to process an order")
                .register(meterRegistry);
    }

    public void orderCreated() {
        ordersCreated.increment();
    }

    public void inventoryReserved() {
        inventoryReserved.increment();
    }

    public void inventoryFailed() {
        inventoryFailed.increment();
    }

    public void paymentSucceeded() {
        paymentSucceeded.increment();
    }

    public void paymentFailed() {
        paymentFailed.increment();
    }

    public void orderConfirmed() {
        ordersConfirmed.increment();
    }

    public void orderCancelled() {
        ordersCancelled.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }

    public void recordProcessing(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
    }
}
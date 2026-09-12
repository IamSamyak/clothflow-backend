package com.clothflow.inventory.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InventoryMetrics {

    private final Counter reservationsCreated;
    private final Counter reservationsReleased;
    private final Counter reservationsCommitted;
    private final Counter insufficientStock;
    private final Counter stockAdjustments;

    public InventoryMetrics(MeterRegistry meterRegistry) {

        reservationsCreated = Counter.builder("inventory.reservations.created")
                .description("Number of inventory reservations created")
                .register(meterRegistry);

        reservationsReleased = Counter.builder("inventory.reservations.released")
                .description("Number of inventory reservations released")
                .register(meterRegistry);

        reservationsCommitted = Counter.builder("inventory.reservations.committed")
                .description("Number of inventory reservations committed")
                .register(meterRegistry);

        insufficientStock = Counter.builder("inventory.stock.insufficient")
                .description("Number of requests rejected due to insufficient stock")
                .register(meterRegistry);

        stockAdjustments = Counter.builder("inventory.stock.adjustments")
                .description("Number of stock adjustment operations")
                .register(meterRegistry);
    }

    public void reservationCreated() {
        reservationsCreated.increment();
    }

    public void reservationReleased() {
        reservationsReleased.increment();
    }

    public void reservationCommitted() {
        reservationsCommitted.increment();
    }

    public void insufficientStock() {
        insufficientStock.increment();
    }

    public void stockAdjustment() {
        stockAdjustments.increment();
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationTransactionIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldAllowOnlyOneConcurrentReservation()
            throws Exception {

        UUID productId = UUID.randomUUID();

        // ---------------------------------------------------------
        // 1. Create inventory with 10 units
        // ---------------------------------------------------------

        inventoryService.createInventory(
                new CreateInventoryRequest(
                        productId,
                        10L
                )
        );

        Inventory initialInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        UUID inventoryId =
                initialInventory.getId();

        // ---------------------------------------------------------
        // 2. Prepare two concurrent reservation requests
        //
        // Both requests want 7 units.
        //
        // Available stock = 10
        //
        // Therefore only ONE reservation can succeed.
        // ---------------------------------------------------------

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<Void> reserveTask =
                () -> {

                    startSignal.await();

                    inventoryService.reserveStock(
                            productId,
                            new ReservationRequest(
                                    UUID.randomUUID(),
                                    7L
                            )
                    );

                    return null;
                };

        Future<Void> futureA =
                executor.submit(reserveTask);

        Future<Void> futureB =
                executor.submit(reserveTask);

        // ---------------------------------------------------------
        // 3. Release both threads at approximately the same time
        // ---------------------------------------------------------

        startSignal.countDown();

        int successCount = 0;
        int failureCount = 0;

        try {
            futureA.get();
            successCount++;
        } catch (ExecutionException exception) {
            failureCount++;
        }

        try {
            futureB.get();
            successCount++;
        } catch (ExecutionException exception) {
            failureCount++;
        }

        executor.shutdown();

        // ---------------------------------------------------------
        // 4. Exactly one reservation should succeed
        // ---------------------------------------------------------

        assertThat(successCount)
                .isEqualTo(1);

        assertThat(failureCount)
                .isEqualTo(1);

        // ---------------------------------------------------------
        // 5. Verify inventory state
        // ---------------------------------------------------------

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        /*
         * Physical stock must remain unchanged.
         */
        assertThat(inventory.getQuantity())
                .isEqualTo(10L);

        /*
         * Only one reservation of 7 should exist.
         */
        assertThat(inventory.getReservedQuantity())
                .isEqualTo(7L);

        /*
         * Available = quantity - reservedQuantity
         *
         * 10 - 7 = 3
         */
        assertThat(
                inventory.getQuantity()
                        - inventory.getReservedQuantity()
        ).isEqualTo(3L);

        // ---------------------------------------------------------
        // 6. Verify exactly one ACTIVE reservation belongs
        //    to this inventory
        //
        // IMPORTANT:
        // We query by inventoryId instead of calling
        // reservation.getInventory().getProductId().
        //
        // This avoids accessing the LAZY association after
        // the Hibernate session has closed.
        // ---------------------------------------------------------

        List<?> reservations =
                reservationRepository
                        .findByInventoryId(inventoryId);

        long activeReservations =
                reservations.stream()
                        .filter(reservation ->
                                ((com.clothflow.inventory.entity.InventoryReservation) reservation)
                                        .getStatus()
                                        .name()
                                        .equals("ACTIVE")
                        )
                        .count();

        assertThat(activeReservations)
                .isEqualTo(1);
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.entity.InventoryReservation;
import com.clothflow.inventory.entity.ReservationStatus;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationDuplicateConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldProcessConcurrentDuplicateReservationOnlyOnce()
            throws Exception {

        UUID productId =
                UUID.randomUUID();

        UUID reservationId =
                UUID.randomUUID();

        /*
         * Initial inventory:
         *
         * quantity = 100
         * reserved = 0
         * available = 100
         */
        Inventory inventory =
                new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(0L);

        inventory =
                inventoryRepository.saveAndFlush(
                        inventory);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Callable<Exception> reservationTask =
                () -> {

                    startLatch.await();

                    try {

                        inventoryService.reserveStock(
                                productId,
                                new ReservationRequest(
                                        reservationId,
                                        20L
                                )
                        );

                        return null;

                    } catch (Exception e) {

                        return e;
                    }
                };

        Future<Exception> first =
                executor.submit(
                        reservationTask);

        Future<Exception> second =
                executor.submit(
                        reservationTask);

        /*
         * Release both threads at approximately
         * the same time.
         */
        startLatch.countDown();

        Exception firstException =
                first.get();

        Exception secondException =
                second.get();

        executor.shutdown();

        /*
         * Both requests should succeed.
         *
         * The second request is an idempotent retry,
         * not a second reservation.
         */
        assertThat(firstException)
                .isNull();

        assertThat(secondException)
                .isNull();

        /*
         * Reload inventory from PostgreSQL.
         */
        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        /*
         * Only ONE reservation of 20 must exist.
         */
        assertThat(
                finalInventory.getQuantity())
                .isEqualTo(100L);

        assertThat(
                finalInventory.getReservedQuantity())
                .isEqualTo(20L);

        assertThat(
                finalInventory.getQuantity()
                        - finalInventory.getReservedQuantity())
                .isEqualTo(80L);

        /*
         * Verify only one reservation row exists.
         */
        List<InventoryReservation> reservations =
                reservationRepository
                        .findByInventoryId(
                                finalInventory.getId());

        assertThat(reservations)
                .hasSize(1);

        InventoryReservation reservation =
                reservations.get(0);

        assertThat(
                reservation.getReservationId())
                .isEqualTo(reservationId);

        assertThat(
                reservation.getQuantity())
                .isEqualTo(20L);

        assertThat(
                reservation.getStatus())
                .isEqualTo(
                        ReservationStatus.ACTIVE);
    }
}
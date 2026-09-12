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

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationCommitReleaseConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldAllowOnlyOneOfCommitOrReleaseToWin()
            throws Exception {

        UUID productId =
                UUID.randomUUID();

        UUID reservationId =
                UUID.randomUUID();

        /*
         * Initial inventory:
         *
         * quantity = 100
         * reserved = 20
         * available = 80
         */
        Inventory inventory =
                new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(20L);

        inventory =
                inventoryRepository.saveAndFlush(
                        inventory);

        /*
         * Create ACTIVE reservation for 20.
         */
        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(
                reservationId);

        reservation.setInventory(
                inventory);

        reservation.setQuantity(20L);

        reservation.setStatus(
                ReservationStatus.ACTIVE);

        reservationRepository.saveAndFlush(
                reservation);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        /*
         * Thread A → COMMIT
         */
        Callable<Exception> commitTask =
                () -> {

                    startLatch.await();

                    try {

                        inventoryService.commitReservation(
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

        /*
         * Thread B → RELEASE
         */
        Callable<Exception> releaseTask =
                () -> {

                    startLatch.await();

                    try {

                        inventoryService.releaseReservation(
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

        Future<Exception> commitFuture =
                executor.submit(commitTask);

        Future<Exception> releaseFuture =
                executor.submit(releaseTask);

        /*
         * Start both operations approximately together.
         */
        startLatch.countDown();

        Exception commitException =
                commitFuture.get();

        Exception releaseException =
                releaseFuture.get();

        executor.shutdown();

        /*
         * Exactly one operation should succeed.
         */
        int successfulOperations =
                (commitException == null ? 1 : 0)
                        + (releaseException == null ? 1 : 0);

        assertThat(successfulOperations)
                .isEqualTo(1);

        /*
         * Reload everything from PostgreSQL.
         */
        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        InventoryReservation finalReservation =
                reservationRepository
                        .findByReservationId(
                                reservationId)
                        .orElseThrow();

        /*
         * Reservation must be in exactly one terminal state.
         */
        assertThat(
                finalReservation.getStatus())
                .isIn(
                        ReservationStatus.COMMITTED,
                        ReservationStatus.RELEASED
                );

        /*
         * Regardless of which operation wins,
         * reserved quantity must be zero.
         */
        assertThat(
                finalInventory.getReservedQuantity())
                .isEqualTo(0L);

        /*
         * The final quantity tells us which operation won.
         *
         * COMMIT:
         *   quantity = 80
         *
         * RELEASE:
         *   quantity = 100
         */
        if (finalReservation.getStatus()
                == ReservationStatus.COMMITTED) {

            assertThat(
                    finalInventory.getQuantity())
                    .isEqualTo(80L);

        } else {

            assertThat(
                    finalInventory.getQuantity())
                    .isEqualTo(100L);
        }

        /*
         * Available quantity must always remain valid.
         */
        assertThat(
                finalInventory.getQuantity()
                        - finalInventory.getReservedQuantity())
                .isEqualTo(
                        finalInventory.getQuantity());
    }
}
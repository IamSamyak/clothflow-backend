package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.entity.InventoryReservation;
import com.clothflow.inventory.entity.ReservationStatus;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class InventoryReservationCompletionConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldAllowOnlyOneConcurrentReservationCompletion()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(20L);

        inventory = inventoryRepository.saveAndFlush(inventory);

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(20L);
        reservation.setStatus(ReservationStatus.ACTIVE);

        reservationRepository.saveAndFlush(reservation);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Future<?> releaseFuture =
                executor.submit(() -> {

                    await(startLatch);

                    inventoryService.releaseReservation(
                            productId,
                            new com.clothflow.inventory.dto.ReservationRequest(
                                    reservationId,
                                    20L
                            )
                    );
                });

        Future<?> commitFuture =
                executor.submit(() -> {

                    await(startLatch);

                    inventoryService.commitReservation(
                            productId,
                            new com.clothflow.inventory.dto.ReservationRequest(
                                    reservationId,
                                    20L
                            )
                    );
                });

        startLatch.countDown();

        int successes = 0;
        int failures = 0;

        try {
            releaseFuture.get();
            successes++;
        } catch (ExecutionException e) {
            failures++;
        }

        try {
            commitFuture.get();
            successes++;
        } catch (ExecutionException e) {
            failures++;
        }

        executor.shutdown();

        assertEquals(1, successes);
        assertEquals(1, failures);

        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        InventoryReservation finalReservation =
                reservationRepository
                        .findByReservationId(reservationId)
                        .orElseThrow();

        assertEquals(
                0L,
                finalInventory.getReservedQuantity()
        );

        assertTrue(
                finalReservation.getStatus()
                        == ReservationStatus.RELEASED
                        ||
                        finalReservation.getStatus()
                                == ReservationStatus.COMMITTED
        );

        if (finalReservation.getStatus()
                == ReservationStatus.RELEASED) {

            assertEquals(
                    100L,
                    finalInventory.getQuantity()
            );
        }

        if (finalReservation.getStatus()
                == ReservationStatus.COMMITTED) {

            assertEquals(
                    80L,
                    finalInventory.getQuantity()
            );
        }
    }

    private void await(CountDownLatch latch) {

        try {
            latch.await();
        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(e);
        }
    }
}
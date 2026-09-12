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

import static org.junit.jupiter.api.Assertions.*;

class InventoryReservationCommitIdempotencyConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void shouldCommitReservationOnlyOnceUnderConcurrentRequests()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        // ---------------------------------------------------------
        // 1. Create initial inventory
        // ---------------------------------------------------------

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(20L);

        inventory =
                inventoryRepository.saveAndFlush(inventory);

        // ---------------------------------------------------------
        // 2. Create ACTIVE reservation for 20 units
        // ---------------------------------------------------------

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(20L);
        reservation.setStatus(
                ReservationStatus.ACTIVE
        );

        reservationRepository.saveAndFlush(
                reservation
        );

        // ---------------------------------------------------------
        // 3. Prepare two concurrent commit requests
        // ---------------------------------------------------------

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Callable<Exception> commitTask = () -> {

            try {

                // Both threads wait here so they start
                // the commit operation at approximately
                // the same time.
                startLatch.await();

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

        Future<Exception> first =
                executor.submit(commitTask);

        Future<Exception> second =
                executor.submit(commitTask);

        // ---------------------------------------------------------
        // 4. Release both threads
        // ---------------------------------------------------------

        startLatch.countDown();

        Exception firstException =
                first.get();

        Exception secondException =
                second.get();

        executor.shutdown();

        // ---------------------------------------------------------
        // 5. Both requests may successfully return.
        //
        // The second request is an idempotent retry because
        // the reservation is already COMMITTED.
        // ---------------------------------------------------------

        assertNull(firstException);
        assertNull(secondException);

        // ---------------------------------------------------------
        // 6. Verify inventory was committed ONLY ONCE
        // ---------------------------------------------------------

        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertEquals(
                80L,
                finalInventory.getQuantity()
        );

        assertEquals(
                0L,
                finalInventory.getReservedQuantity()
        );

        // ---------------------------------------------------------
        // 7. Verify reservation reached COMMITTED exactly once
        // ---------------------------------------------------------

        InventoryReservation finalReservation =
                reservationRepository
                        .findByReservationId(reservationId)
                        .orElseThrow();

        assertEquals(
                ReservationStatus.COMMITTED,
                finalReservation.getStatus()
        );
    }
}
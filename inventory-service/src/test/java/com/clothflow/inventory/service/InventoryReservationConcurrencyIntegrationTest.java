package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.entity.InventoryReservation;
import com.clothflow.inventory.entity.ReservationStatus;
import com.clothflow.inventory.exception.InsufficientStockException;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    AtomicInteger requestNumber =
            new AtomicInteger();

    @Test
    void shouldAllowOnlyOneReservationWhenConcurrentRequestsExceedStock()
            throws Exception {

        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(0L);

        inventoryRepository.saveAndFlush(inventory);

        UUID reservationA = UUID.randomUUID();
        UUID reservationB = UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        AtomicInteger requestNumber =
                new AtomicInteger();

        Callable<Boolean> reservationTask =
                () -> {

                    startLatch.await();

                    int request =
                            requestNumber.getAndIncrement();

                    UUID reservationId =
                            request == 0
                                    ? reservationA
                                    : reservationB;

                    try {

                        inventoryService.reserveStock(
                                productId,
                                new ReservationRequest(
                                        reservationId,
                                        60L
                                )
                        );

                        return true;

                    } catch (InsufficientStockException e) {

                        return false;
                    }
                };

        Future<Boolean> first =
                executor.submit(reservationTask);

        Future<Boolean> second =
                executor.submit(reservationTask);

        startLatch.countDown();

        boolean firstSucceeded =
                first.get();

        boolean secondSucceeded =
                second.get();

        executor.shutdown();

        /*
         * 100 units exist.
         *
         * Each request wants 60.
         *
         * Therefore exactly one request can succeed.
         */
        assertThat(
                firstSucceeded
                        ? 1
                        : 0
                        + (secondSucceeded ? 1 : 0)
        ).isEqualTo(1);

        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(
                finalInventory.getQuantity())
                .isEqualTo(100L);

        assertThat(
                finalInventory.getReservedQuantity())
                .isEqualTo(60L);

        assertThat(
                finalInventory.getQuantity()
                        - finalInventory.getReservedQuantity())
                .isEqualTo(40L);

        long activeReservations =
                reservationRepository
                        .findByInventoryId(
                                finalInventory.getId())
                        .stream()
                        .filter(r ->
                                r.getStatus()
                                        == ReservationStatus.ACTIVE)
                        .count();

        assertThat(activeReservations)
                .isEqualTo(1);
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryIdempotencyConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldHandleConcurrentDuplicateReservationRequests()
            throws Exception {

        UUID productId = UUID.randomUUID();

        UUID reservationId = UUID.randomUUID();

        inventoryService.createInventory(
                new CreateInventoryRequest(
                        productId,
                        10L
                )
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<InventoryResponse> reserveTask =
                () -> {

                    startSignal.await();

                    return inventoryService.reserveStock(
                            productId,
                            new ReservationRequest(
                                    reservationId,
                                    3L
                            )
                    );
                };

        Future<InventoryResponse> requestA =
                executor.submit(reserveTask);

        Future<InventoryResponse> requestB =
                executor.submit(reserveTask);

        startSignal.countDown();

        int successCount = 0;
        int failureCount = 0;

        try {
            requestA.get();
            successCount++;
        } catch (ExecutionException exception) {
            failureCount++;
        }

        try {
            requestB.get();
            successCount++;
        } catch (ExecutionException exception) {
            failureCount++;
        }

        executor.shutdown();

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(inventory.getQuantity())
                .isEqualTo(10L);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(3L);

        assertThat(
                successCount + failureCount
        ).isEqualTo(2);
    }
}
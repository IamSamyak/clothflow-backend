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

class InventoryPessimisticLockIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldSerializeConcurrentReservationsUsingPessimisticLock()
            throws Exception {

        UUID productId = UUID.randomUUID();

        inventoryService.createInventory(
                new CreateInventoryRequest(
                        productId,
                        10L
                )
        );

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<InventoryResponse> reserveTask =
                () -> {
                    startSignal.await();

                    return inventoryService
                            .reserveStockPessimistically(
                                    productId,
                                    new ReservationRequest(
                                            UUID.randomUUID(),
                                            7L
                                    )
                            );
                };

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(2)) {

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

            assertThat(successCount)
                    .isEqualTo(1);

            assertThat(failureCount)
                    .isEqualTo(1);
        }

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(inventory.getQuantity())
                .isEqualTo(10L);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(7L);

        assertThat(
                inventory.getQuantity()
                        - inventory.getReservedQuantity()
        ).isEqualTo(3L);
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.exception.InsufficientStockException;
import com.clothflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldPreventOversellingWhenTwoRequestsReserveSimultaneously()
            throws Exception {

        // Arrange
        UUID productId = UUID.randomUUID();

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

        Callable<InventoryResponse> reserveTask = () -> {

            startSignal.await();

            return inventoryService.reserveStock(
                    productId,
                    new ReservationRequest(
                            UUID.randomUUID(),
                            7L
                    )
            );
        };

        Future<InventoryResponse> requestA =
                executor.submit(reserveTask);

        Future<InventoryResponse> requestB =
                executor.submit(reserveTask);

        // Start both requests at approximately the same time
        startSignal.countDown();

        int successfulReservations = 0;
        int insufficientStockFailures = 0;

        try {
            requestA.get();
            successfulReservations++;
        } catch (ExecutionException e) {

            if (e.getCause() instanceof InsufficientStockException) {
                insufficientStockFailures++;
            } else {
                throw e;
            }
        }

        try {
            requestB.get();
            successfulReservations++;
        } catch (ExecutionException e) {

            if (e.getCause() instanceof InsufficientStockException) {
                insufficientStockFailures++;
            } else {
                throw e;
            }
        }

        executor.shutdown();

        // Assert
        assertThat(successfulReservations)
                .isEqualTo(1);

        assertThat(insufficientStockFailures)
                .isEqualTo(1);

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

        assertThat(inventory.getVersion())
                .isEqualTo(1L);
    }

    private boolean isOptimisticLockFailure(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof ObjectOptimisticLockingFailureException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
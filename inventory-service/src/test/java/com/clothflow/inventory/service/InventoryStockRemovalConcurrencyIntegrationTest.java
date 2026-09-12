package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.StockAdjustmentRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.exception.InsufficientStockException;
import com.clothflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryStockRemovalConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    AtomicInteger requestNumber =
            new AtomicInteger();

    @Test
    void shouldAllowOnlyOneConcurrentRemovalWhenStockIsInsufficientForBoth()
            throws Exception {

        UUID productId =
                UUID.randomUUID();

        Inventory inventory =
                new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(0L);

        inventoryRepository.saveAndFlush(inventory);

        UUID operationA =
                UUID.randomUUID();

        UUID operationB =
                UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        AtomicInteger requestNumber =
                new AtomicInteger();

        Callable<Boolean> removeTask =
                () -> {

                    startLatch.await();

                    int request =
                            requestNumber.getAndIncrement();

                    UUID operationId =
                            request == 0
                                    ? operationA
                                    : operationB;

                    try {

                        inventoryService.removeStock(
                                productId,
                                new StockAdjustmentRequest(
                                        operationId,
                                        70L
                                )
                        );

                        return true;

                    } catch (InsufficientStockException e) {

                        return false;
                    }
                };;

        Future<Boolean> first =
                executor.submit(removeTask);

        Future<Boolean> second =
                executor.submit(removeTask);

        startLatch.countDown();

        boolean firstSucceeded =
                first.get();

        boolean secondSucceeded =
                second.get();

        executor.shutdown();

        int successfulOperations =
                (firstSucceeded ? 1 : 0)
                        + (secondSucceeded ? 1 : 0);

        assertThat(successfulOperations)
                .isEqualTo(1);

        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(
                finalInventory.getQuantity())
                .isEqualTo(30L);

        assertThat(
                finalInventory.getReservedQuantity())
                .isEqualTo(0L);

        assertThat(
                finalInventory.getQuantity()
                        - finalInventory.getReservedQuantity())
                .isEqualTo(30L);
    }
}
package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.StockAdjustmentRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryStockAdjustmentConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldPreserveBothConcurrentAddAndRemoveOperations()
            throws Exception {

        UUID productId =
                UUID.randomUUID();

        Inventory inventory =
                new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(0L);

        inventoryRepository.saveAndFlush(inventory);

        UUID addOperationId =
                UUID.randomUUID();

        UUID removeOperationId =
                UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        AtomicInteger requestNumber =
                new AtomicInteger();

        Callable<Exception> adjustmentTask =
                () -> {

                    startLatch.await();

                    int request =
                            requestNumber.getAndIncrement();

                    try {

                        if (request == 0) {

                            inventoryService.addStock(
                                    productId,
                                    new StockAdjustmentRequest(
                                            addOperationId,
                                            50L
                                    )
                            );

                        } else {

                            inventoryService.removeStock(
                                    productId,
                                    new StockAdjustmentRequest(
                                            removeOperationId,
                                            30L
                                    )
                            );
                        }

                        return null;

                    } catch (Exception e) {

                        return e;
                    }
                };

        Future<Exception> first =
                executor.submit(adjustmentTask);

        Future<Exception> second =
                executor.submit(adjustmentTask);

        startLatch.countDown();

        Exception firstException =
                first.get();

        Exception secondException =
                second.get();

        executor.shutdown();

        assertThat(firstException)
                .isNull();

        assertThat(secondException)
                .isNull();

        Inventory finalInventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        /*
         * 100 + 50 - 30 = 120
         */
        assertThat(
                finalInventory.getQuantity())
                .isEqualTo(120L);

        assertThat(
                finalInventory.getReservedQuantity())
                .isEqualTo(0L);

        assertThat(
                finalInventory.getQuantity()
                        - finalInventory.getReservedQuantity())
                .isEqualTo(120L);
    }
}
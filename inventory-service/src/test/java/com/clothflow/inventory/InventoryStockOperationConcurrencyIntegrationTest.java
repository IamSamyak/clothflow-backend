package com.clothflow.inventory;

import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.StockAdjustmentRequest;
import com.clothflow.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class InventoryStockOperationConcurrencyIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void sameOperationIdConcurrentlyShouldApplyStockOnlyOnce()
            throws Exception {

        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        transactionTemplate.execute(status -> {

            inventoryService.createInventory(
                    new CreateInventoryRequest(
                            productId,
                            10L
                    )
            );

            return null;
        });

        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        operationId,
                        5L
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Future<InventoryResponse> first =
                executor.submit(() -> {

                    startLatch.await();

                    return inventoryService.addStock(
                            productId,
                            request
                    );
                });

        Future<InventoryResponse> second =
                executor.submit(() -> {

                    startLatch.await();

                    return inventoryService.addStock(
                            productId,
                            request
                    );
                });

        startLatch.countDown();

        InventoryResponse response1 = getResult(first);
        InventoryResponse response2 = getResult(second);

        executor.shutdown();

        InventoryResponse finalInventory =
                transactionTemplate.execute(status ->
                        inventoryService.getInventory(productId)
                );

        assertNotNull(finalInventory);

        assertEquals(
                15L,
                finalInventory.quantity()
        );

        assertEquals(
                0L,
                finalInventory.reservedQuantity()
        );

        assertEquals(
                15L,
                finalInventory.availableQuantity()
        );
    }

    private InventoryResponse getResult(
            Future<InventoryResponse> future)
            throws Exception {

        try {
            return future.get(
                    10,
                    TimeUnit.SECONDS
            );
        } catch (ExecutionException e) {

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw e;
        }
    }
}
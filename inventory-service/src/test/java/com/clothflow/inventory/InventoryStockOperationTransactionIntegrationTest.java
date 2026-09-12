package com.clothflow.inventory;

import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.StockAdjustmentRequest;
import com.clothflow.inventory.entity.InventoryStockOperation;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryStockOperationRepository;
import com.clothflow.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class InventoryStockOperationTransactionIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryStockOperationRepository
            stockOperationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void stockOperationAndInventoryChangeShouldRollbackTogether() {

        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        transactionTemplate.execute(status -> {

            inventoryService.createInventory(
                    new CreateInventoryRequest(
                            productId,
                            100L
                    )
            );

            return null;
        });

        StockAdjustmentRequest request =
                new StockAdjustmentRequest(
                        operationId,
                        50L
                );

        assertThrows(
                RuntimeException.class,
                () -> inventoryService.addStockAndFail(
                        productId,
                        request
                )
        );

        InventoryResponse inventory =
                transactionTemplate.execute(status ->
                        inventoryService.getInventory(productId)
                );

        assertEquals(
                100L,
                inventory.quantity()
        );

        assertEquals(
                0L,
                inventory.reservedQuantity()
        );

        assertEquals(
                100L,
                inventory.availableQuantity()
        );

        assertTrue(
                stockOperationRepository
                        .findByOperationId(operationId)
                        .isEmpty()
        );
    }
}
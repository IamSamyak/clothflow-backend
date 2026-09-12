package com.clothflow.inventory.repository;

import com.clothflow.inventory.entity.InventoryStockOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryStockOperationRepository
        extends JpaRepository<
        InventoryStockOperation,
        UUID> {

    Optional<InventoryStockOperation>
    findByOperationId(UUID operationId);

    @Modifying
    @Query(
            value = """
                INSERT INTO inventory_stock_operation
                    (
                        id,
                        operation_id,
                        inventory_id,
                        operation_type,
                        quantity
                    )
                VALUES
                    (
                        :id,
                        :operationId,
                        :inventoryId,
                        :operationType,
                        :quantity
                    )
                ON CONFLICT (operation_id)
                DO NOTHING
                """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("operationId") UUID operationId,
            @Param("inventoryId") UUID inventoryId,
            @Param("operationType") String operationType,
            @Param("quantity") Long quantity
    );
}
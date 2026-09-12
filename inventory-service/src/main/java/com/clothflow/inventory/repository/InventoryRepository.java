package com.clothflow.inventory.repository;

import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.entity.InventoryStockOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository
        extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.productId = :productId
            """)
    Optional<Inventory> findByProductIdForUpdate(
            @Param("productId") UUID productId
    );
}
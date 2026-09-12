package com.clothflow.inventory.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory_stock_operation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_operation_id",
                        columnNames = "operation_id"
                )
        }
)
public class InventoryStockOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "operation_id",
            nullable = false,
            unique = true
    )
    private UUID operationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inventory_id",
            nullable = false
    )
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "operation_type",
            nullable = false,
            length = 20
    )
    private StockOperationType operationType;

    @Column(nullable = false)
    private Long quantity;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public InventoryStockOperation() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public StockOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(
            StockOperationType operationType) {

        this.operationType = operationType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
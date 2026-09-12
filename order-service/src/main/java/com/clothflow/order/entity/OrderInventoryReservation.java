package com.clothflow.order.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "order_inventory_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_inventory_reservation_id",
                        columnNames = "reservation_id"
                ),
                @UniqueConstraint(
                        name = "uk_order_inventory_order_item",
                        columnNames = "order_item_id"
                )
        }
)
public class OrderInventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_inventory_reservation_order"
            )
    )
    private Order order;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_item_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_inventory_reservation_order_item"
            )
    )
    private OrderItem orderItem;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OrderInventoryReservation() {
    }

    public OrderInventoryReservation(
            Order order,
            OrderItem orderItem,
            UUID reservationId,
            UUID productId,
            Integer quantity
    ) {
        this.order = order;
        this.orderItem = orderItem;
        this.reservationId = reservationId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = InventoryReservationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
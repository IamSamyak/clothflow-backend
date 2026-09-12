package com.clothflow.order.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "order_status_history",
        indexes = {
                @Index(
                        name = "idx_order_status_history_order_id",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_status_history_created_at",
                        columnList = "created_at"
                )
        }
)
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OrderStatusHistory() {
        // Required by JPA
    }

    public OrderStatusHistory(
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason
    ) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
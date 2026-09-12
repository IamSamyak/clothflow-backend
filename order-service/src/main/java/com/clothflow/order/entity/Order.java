package com.clothflow.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Setter
    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Setter
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @Column(
            name = "recipient_name",
            nullable = false,
            length = 200
    )
    private String recipientName;

    @Column(
            name = "shipping_address_line1",
            nullable = false,
            length = 255
    )
    private String shippingAddressLine1;

    @Column(
            name = "shipping_address_line2",
            length = 255
    )
    private String shippingAddressLine2;

    @Column(
            name = "shipping_city",
            nullable = false,
            length = 100
    )
    private String shippingCity;

    @Column(
            name = "shipping_state",
            nullable = false,
            length = 100
    )
    private String shippingState;

    @Column(
            name = "shipping_postal_code",
            nullable = false,
            length = 20
    )
    private String shippingPostalCode;

    @Column(
            name = "shipping_country",
            nullable = false,
            length = 100
    )
    private String shippingCountry;

    protected Order() {
        // Required by JPA
    }

    public Order(
            String orderNumber,
            UUID customerId,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            String recipientName,
            String shippingAddressLine1,
            String shippingAddressLine2,
            String shippingCity,
            String shippingState,
            String shippingPostalCode,
            String shippingCountry
    ) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;

        this.recipientName = recipientName;
        this.shippingAddressLine1 = shippingAddressLine1;
        this.shippingAddressLine2 = shippingAddressLine2;
        this.shippingCity = shippingCity;
        this.shippingState = shippingState;
        this.shippingPostalCode = shippingPostalCode;
        this.shippingCountry = shippingCountry;

        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistory history) {
        statusHistory.add(history);
        history.setOrder(this);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void assignShipment(UUID shipmentId) {
        if (shipmentId == null) {
            throw new IllegalArgumentException(
                    "shipmentId must not be null"
            );
        }

        if (this.shipmentId != null &&
                !this.shipmentId.equals(shipmentId)) {
            throw new IllegalStateException(
                    "Order " + this.id +
                            " is already associated with shipment " +
                            this.shipmentId
            );
        }

        this.shipmentId = shipmentId;
    }
}
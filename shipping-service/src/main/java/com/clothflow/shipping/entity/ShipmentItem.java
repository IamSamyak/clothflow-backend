package com.clothflow.shipping.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "shipment_items",
        indexes = {
                @Index(
                        name = "idx_shipment_items_shipment_id",
                        columnList = "shipment_id"
                ),
                @Index(
                        name = "idx_shipment_items_product_id",
                        columnList = "product_id"
                )
        }
)
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "shipment_id",
            nullable = false
    )
    private Shipment shipment;

    @Column(
            name = "product_id",
            nullable = false,
            updatable = false
    )
    private UUID productId;

    @Column(
            name = "product_name",
            nullable = false,
            length = 255,
            updatable = false
    )
    private String productName;

    @Column(
            nullable = false
    )
    private Integer quantity;


    protected ShipmentItem() {
        // Required by JPA
    }


    public ShipmentItem(
            UUID productId,
            String productName,
            Integer quantity
    ) {

        if (quantity == null ||
                quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }


    void setShipment(
            Shipment shipment
    ) {

        this.shipment = shipment;
    }


    public UUID getId() {
        return id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
package com.clothflow.shipping.entity;

import com.clothflow.shipping.exception.InvalidShipmentStatusTransitionException;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(
                        name = "idx_shipments_customer_id",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_shipments_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_shipments_tracking_number",
                        columnList = "tracking_number"
                ),
                @Index(
                        name = "idx_shipments_created_at",
                        columnList = "created_at"
                )
        }
)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "order_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID orderId;

    @Column(
            name = "customer_id",
            nullable = false,
            updatable = false
    )
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private ShipmentStatus status;

    @Column(
            length = 100
    )
    private String carrier;

    @Column(
            name = "tracking_number",
            length = 100
    )
    private String trackingNumber;

    @Column(
            name = "recipient_name",
            nullable = false,
            length = 200
    )
    private String recipientName;

    @Column(
            name = "address_line1",
            nullable = false,
            length = 255
    )
    private String addressLine1;

    @Column(
            name = "address_line2",
            length = 255
    )
    private String addressLine2;

    @Column(
            nullable = false,
            length = 100
    )
    private String city;

    @Column(
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "postal_code",
            nullable = false,
            length = 20
    )
    private String postalCode;

    @Column(
            nullable = false,
            length = 100
    )
    private String country;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @OneToMany(
            mappedBy = "shipment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ShipmentItem> items =
            new ArrayList<>();


    protected Shipment() {
        // Required by JPA
    }


    public Shipment(
            UUID orderId,
            UUID customerId,
            String recipientName,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country
    ) {

        this.orderId = orderId;
        this.customerId = customerId;

        this.status = ShipmentStatus.PENDING;

        this.recipientName = recipientName;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;

        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }


    public void addItem(
            ShipmentItem item
    ) {

        items.add(item);

        item.setShipment(this);
    }


    public void startProcessing() {

        transitionTo(
                ShipmentStatus.PROCESSING
        );
    }


    public void markShipped(
            String carrier,
            String trackingNumber
    ) {

        if (carrier == null ||
                carrier.isBlank()) {

            throw new IllegalArgumentException(
                    "Carrier is required"
            );
        }

        if (trackingNumber == null ||
                trackingNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Tracking number is required"
            );
        }

        transitionTo(
                ShipmentStatus.SHIPPED
        );

        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = OffsetDateTime.now();
    }


    public void markOutForDelivery() {

        transitionTo(
                ShipmentStatus.OUT_FOR_DELIVERY
        );
    }


    public void markDelivered() {

        transitionTo(
                ShipmentStatus.DELIVERED
        );

        this.deliveredAt = OffsetDateTime.now();
    }


    public void markDeliveryFailed() {

        transitionTo(
                ShipmentStatus.DELIVERY_FAILED
        );
    }


    public void retryDelivery() {

        transitionTo(
                ShipmentStatus.PROCESSING
        );
    }


    public void cancel() {

        transitionTo(
                ShipmentStatus.CANCELLED
        );
    }


    private void transitionTo(
            ShipmentStatus targetStatus
    ) {

        if (!isTransitionAllowed(
                this.status,
                targetStatus
        )) {

            throw new InvalidShipmentStatusTransitionException(
                    this.status,
                    targetStatus
            );
        }

        this.status = targetStatus;
    }


    private boolean isTransitionAllowed(
            ShipmentStatus current,
            ShipmentStatus target
    ) {

        return switch (current) {

            case PENDING ->
                    target == ShipmentStatus.PROCESSING
                            || target == ShipmentStatus.CANCELLED;

            case PROCESSING ->
                    target == ShipmentStatus.SHIPPED
                            || target == ShipmentStatus.CANCELLED;

            case SHIPPED ->
                    target == ShipmentStatus.OUT_FOR_DELIVERY
                            || target == ShipmentStatus.DELIVERY_FAILED;

            case OUT_FOR_DELIVERY ->
                    target == ShipmentStatus.DELIVERED
                            || target == ShipmentStatus.DELIVERY_FAILED;

            case DELIVERY_FAILED ->
                    target == ShipmentStatus.PROCESSING;

            case DELIVERED,
                 CANCELLED ->
                    false;
        };
    }


    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                OffsetDateTime.now();
    }


    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ShipmentItem> getItems() {
        return items;
    }
}
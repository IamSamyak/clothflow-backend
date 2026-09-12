package com.clothflow.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "payment_reference",
            nullable = false,
            unique = true,
            length = 50
    )
    private String paymentReference;

    @Column(
            name = "order_id",
            nullable = false
    )
    private UUID orderId;

    @Column(
            name = "customer_id",
            nullable = false
    )
    private UUID customerId;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private PaymentMethod paymentMethod;

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

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )

    private String idempotencyKey;

    protected Payment() {
    }

    public Payment(
            String paymentReference,
            String idempotencyKey,
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod
    ) {
        this.paymentReference = paymentReference;
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.INITIATED;
    }

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now =
                OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                OffsetDateTime.now();
    }

    public boolean matchesRequest(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod
    ) {

        return this.orderId.equals(orderId)
                && this.customerId.equals(customerId)
                && this.amount.compareTo(amount) == 0
                && this.currency.equals(currency)
                && this.paymentMethod == paymentMethod;
    }
}
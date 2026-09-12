package com.clothflow.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_refund"
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "payment_id",
            nullable = false
    )
    private UUID paymentId;

    @Column(
            name = "order_id",
            nullable = false
    )
    private UUID orderId;

    @Column(
            name = "refund_reference",
            nullable = false,
            unique = true,
            length = 50
    )
    private String refundReference;

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
    private PaymentRefundStatus status;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Version
    @Column(
            nullable = false
    )
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

    public PaymentRefund(
            UUID paymentId,
            UUID orderId,
            String refundReference,
            BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundReference = refundReference;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentRefundStatus.PENDING;
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
            UUID paymentId
    ) {
        return this.paymentId.equals(paymentId);
    }
}
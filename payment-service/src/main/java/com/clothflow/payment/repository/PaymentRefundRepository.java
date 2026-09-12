package com.clothflow.payment.repository;

import com.clothflow.payment.entity.PaymentRefund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundRepository
        extends JpaRepository<PaymentRefund, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentRefund> findByPaymentId(
            UUID paymentId
    );

    Optional<PaymentRefund> findByIdempotencyKey(
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM PaymentRefund r
            WHERE r.id = :refundId
            """)
    Optional<PaymentRefund> findByIdForUpdate(
            @Param("refundId") UUID refundId
    );
}
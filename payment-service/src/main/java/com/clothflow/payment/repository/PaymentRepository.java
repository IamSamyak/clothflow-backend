package com.clothflow.payment.repository;

import com.clothflow.payment.entity.Payment;
import com.clothflow.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentReference(
            String paymentReference
    );

    Optional<Payment> findByIdempotencyKey(
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.idempotencyKey = :idempotencyKey
            """)
    Optional<Payment> findByIdempotencyKeyForUpdate(
            @Param("idempotencyKey")
            String idempotencyKey
    );

    Optional<Payment> findByOrderIdAndStatus(
            UUID orderId,
            PaymentStatus status
    );

    List<Payment> findAllByOrderId(
            UUID orderId
    );

    boolean existsByPaymentReference(
            String paymentReference
    );

    boolean existsByIdempotencyKey(
            String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") UUID paymentId
    );

    // ------------------------------------------------------------
    // Customer ownership queries
    // ------------------------------------------------------------

    Optional<Payment> findByIdAndCustomerId(
            UUID paymentId,
            UUID customerId
    );

    List<Payment> findAllByOrderIdAndCustomerId(
            UUID orderId,
            UUID customerId
    );
}
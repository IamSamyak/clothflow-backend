package com.clothflow.order.repository;

import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository
        extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(
            String orderNumber
    );

    boolean existsByOrderNumber(
            String orderNumber
    );

    Optional<Order> findByIdAndCustomerId(
            UUID id,
            UUID customerId
    );

    @Modifying
    @Query("""
            UPDATE Order o
            SET o.status = :newStatus,
                o.version = o.version + 1,
                o.updatedAt = CURRENT_TIMESTAMP
            WHERE o.id = :orderId
              AND o.status = :currentStatus
            """)
    int transitionStatusAtomically(
            @Param("orderId")
            UUID orderId,

            @Param("currentStatus")
            OrderStatus currentStatus,

            @Param("newStatus")
            OrderStatus newStatus
    );
}
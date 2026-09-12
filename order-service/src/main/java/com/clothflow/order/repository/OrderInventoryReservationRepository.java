package com.clothflow.order.repository;

import com.clothflow.order.entity.OrderInventoryReservation;
import com.clothflow.order.entity.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderInventoryReservationRepository
        extends JpaRepository<OrderInventoryReservation, UUID> {

    List<OrderInventoryReservation> findAllByOrderId(UUID orderId);

    List<OrderInventoryReservation> findAllByOrderIdAndStatus(
            UUID orderId,
            InventoryReservationStatus status
    );

    boolean existsByReservationId(UUID reservationId);
}
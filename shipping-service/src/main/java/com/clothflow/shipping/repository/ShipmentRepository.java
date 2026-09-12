package com.clothflow.shipping.repository;

import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository
        extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(
            UUID orderId
    );

    boolean existsByOrderId(
            UUID orderId
    );

    List<Shipment> findByStatusIn(
            Collection<ShipmentStatus> statuses
    );

    Optional<Shipment> findByTrackingNumber(
            String trackingNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Shipment s
            WHERE s.id = :shipmentId
            """)
    Optional<Shipment> findByIdForUpdate(
            @Param("shipmentId") UUID shipmentId
    );

    @Modifying
    @Query("""
            UPDATE Shipment s
            SET s.status =
                    com.clothflow.shipping.entity.ShipmentStatus.SHIPPED,
                s.carrier = :carrier,
                s.trackingNumber = :trackingNumber,
                s.shippedAt = CURRENT_TIMESTAMP,
                s.updatedAt = CURRENT_TIMESTAMP,
                s.version = s.version + 1
            WHERE s.id = :shipmentId
              AND s.status =
                    com.clothflow.shipping.entity.ShipmentStatus.PROCESSING
            """)
    int markShippedAtomically(
            @Param("shipmentId") UUID shipmentId,
            @Param("carrier") String carrier,
            @Param("trackingNumber") String trackingNumber
    );

    Optional<Shipment> findByIdAndCustomerId(
            UUID shipmentId,
            UUID customerId
    );

    Optional<Shipment> findByOrderIdAndCustomerId(
            UUID orderId,
            UUID customerId
    );
}
package com.clothflow.inventory.repository;

import com.clothflow.inventory.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation>
    findByReservationId(UUID reservationId);

    List<InventoryReservation> findByInventoryId(UUID inventoryId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO inventory_reservation
                        (
                            id,
                            reservation_id,
                            inventory_id,
                            quantity,
                            status
                        )
                    VALUES
                        (
                            :id,
                            :reservationId,
                            :inventoryId,
                            :quantity,
                            'ACTIVE'
                        )
                    ON CONFLICT (reservation_id)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("reservationId") UUID reservationId,
            @Param("inventoryId") UUID inventoryId,
            @Param("quantity") Long quantity
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE InventoryReservation r
        SET r.status = com.clothflow.inventory.entity.ReservationStatus.COMMITTED
        WHERE r.reservationId = :reservationId
          AND r.status = com.clothflow.inventory.entity.ReservationStatus.ACTIVE
        """)
    int markCommittedIfActive(
            @Param("reservationId") UUID reservationId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE InventoryReservation r
        SET r.status = com.clothflow.inventory.entity.ReservationStatus.RELEASED
        WHERE r.reservationId = :reservationId
          AND r.status = com.clothflow.inventory.entity.ReservationStatus.ACTIVE
        """)
    int markReleasedIfActive(
            @Param("reservationId") UUID reservationId
    );
}
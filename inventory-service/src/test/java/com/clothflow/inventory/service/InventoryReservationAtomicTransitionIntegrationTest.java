package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.entity.InventoryReservation;
import com.clothflow.inventory.entity.ReservationStatus;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryReservationAtomicTransitionIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    @Transactional
    void shouldAtomicallyTransitionActiveReservationToCommitted() {

        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(100L);
        inventory.setReservedQuantity(20L);

        inventory =
                inventoryRepository.saveAndFlush(inventory);

        UUID reservationId = UUID.randomUUID();

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(20L);
        reservation.setStatus(
                ReservationStatus.ACTIVE
        );

        reservationRepository.saveAndFlush(
                reservation
        );

        int updated =
                reservationRepository.markCommittedIfActive(
                        reservationId
                );

        assertEquals(1, updated);

        InventoryReservation result =
                reservationRepository
                        .findByReservationId(reservationId)
                        .orElseThrow();

        assertEquals(
                ReservationStatus.COMMITTED,
                result.getStatus()
        );
    }

    @Test
    @Transactional
    void shouldNotTransitionAlreadyCommittedReservation() {

        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(80L);
        inventory.setReservedQuantity(0L);

        inventory =
                inventoryRepository.saveAndFlush(inventory);

        UUID reservationId = UUID.randomUUID();

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(20L);
        reservation.setStatus(
                ReservationStatus.COMMITTED
        );

        reservationRepository.saveAndFlush(
                reservation
        );

        int updated =
                reservationRepository.markCommittedIfActive(
                        reservationId
                );

        assertEquals(0, updated);
    }
}
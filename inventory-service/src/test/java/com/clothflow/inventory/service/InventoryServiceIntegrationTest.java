package com.clothflow.inventory.service;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import com.clothflow.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;


    @Test
    void shouldPersistInventoryInPostgres() {

        UUID productId = UUID.randomUUID();

        InventoryResponse response =
                inventoryService.createInventory(
                        new CreateInventoryRequest(
                                productId,
                                10L
                        )
                );

        Inventory saved =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(saved.getProductId())
                .isEqualTo(productId);

        assertThat(saved.getQuantity())
                .isEqualTo(10L);

        assertThat(saved.getReservedQuantity())
                .isEqualTo(0L);

        assertThat(saved.getVersion())
                .isEqualTo(0L);

        assertThat(response.availableQuantity())
                .isEqualTo(10L);
    }


    @Test
    void shouldNotReserveStockTwiceForSameReservationId() {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        inventoryService.createInventory(
                new CreateInventoryRequest(
                        productId,
                        10L
                )
        );

        ReservationRequest request =
                new ReservationRequest(
                        reservationId,
                        3L
                );

        inventoryService.reserveStock(
                productId,
                request
        );

        inventoryService.reserveStock(
                productId,
                request
        );

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(inventory.getQuantity())
                .isEqualTo(10L);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(3L);

        assertThat(
                inventory.getQuantity()
                        - inventory.getReservedQuantity()
        ).isEqualTo(7L);
    }


    @Test
    void shouldRollbackReservationInventoryAndOutboxOnFailure() {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        inventoryService.createInventory(
                new CreateInventoryRequest(
                        productId,
                        10L
                )
        );

        ReservationRequest request =
                new ReservationRequest(
                        reservationId,
                        2L
                );

        assertThrows(
                RuntimeException.class,
                () ->
                        inventoryService.reserveStockAndFail(
                                productId,
                                request
                        )
        );

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow();

        assertThat(inventory.getQuantity())
                .isEqualTo(10L);

        assertThat(inventory.getReservedQuantity())
                .isEqualTo(0L);

        assertThat(
                reservationRepository
                        .findByReservationId(reservationId)
        ).isEmpty();

        assertThat(
                outboxEventRepository.findAll()
        ).noneMatch(
                event ->
                        event.getAggregateId()
                                .equals(reservationId)
        );
    }
}
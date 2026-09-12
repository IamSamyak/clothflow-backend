package com.clothflow.inventory.service;

import com.clothflow.inventory.config.InventoryMetrics;
import com.clothflow.inventory.dto.*;
import com.clothflow.inventory.entity.*;
import com.clothflow.inventory.exception.InsufficientStockException;
import com.clothflow.inventory.exception.InventoryNotFoundException;
import com.clothflow.inventory.exception.InvalidInventoryOperationException;
import com.clothflow.inventory.repository.InventoryRepository;
import com.clothflow.inventory.repository.InventoryReservationRepository;
import com.clothflow.inventory.repository.InventoryStockOperationRepository;
import com.clothflow.inventory.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryStockOperationRepository inventoryStockOperationRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    private InventoryService inventoryService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private InventoryMetrics inventoryMetrics;

    private UUID inventoryId;
    private UUID productId;

    @BeforeEach
    void setUp() {

        inventoryId =
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                );

        productId =
                UUID.fromString(
                        "22222222-2222-2222-2222-222222222222"
                );

        inventoryService =
                new InventoryService(
                        inventoryRepository,
                        reservationRepository,
                        inventoryStockOperationRepository,
                        outboxEventRepository,
                        objectMapper,
                        inventoryMetrics
                );
    }

    private Inventory createInventory(
            long quantity,
            long reservedQuantity) {

        Inventory inventory = new Inventory();

        inventory.setId(inventoryId);
        inventory.setProductId(productId);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        inventory.setVersion(0L);

        return inventory;
    }

    private Inventory createInventory(
            UUID inventoryId,
            UUID productId,
            long quantity,
            long reservedQuantity) {

        Inventory inventory = new Inventory();

        inventory.setId(inventoryId);
        inventory.setProductId(productId);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        inventory.setVersion(0L);

        return inventory;
    }

    private InventoryReservation createReservation(
            UUID reservationId,
            Inventory inventory,
            long quantity,
            ReservationStatus status) {

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(quantity);
        reservation.setStatus(status);

        return reservation;
    }

    // ---------------------------------------------------------
    // CREATE INVENTORY
    // ---------------------------------------------------------

    @Test
    void shouldCreateInventory() {

        CreateInventoryRequest request =
                new CreateInventoryRequest(
                        productId,
                        100L
                );

        Inventory inventory =
                createInventory(100L, 0L);

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(inventory);

        InventoryResponse response =
                inventoryService.createInventory(request);

        assertNotNull(response);

        assertEquals(
                productId,
                response.productId()
        );

        assertEquals(
                100L,
                response.quantity()
        );

        assertEquals(
                0L,
                response.reservedQuantity()
        );

        assertEquals(
                100L,
                response.availableQuantity()
        );

        verify(inventoryRepository)
                .findByProductId(productId);

        verify(inventoryRepository)
                .save(any(Inventory.class));
    }

    @Test
    void shouldReserveStockAndCreateOutboxEvent() throws JsonProcessingException {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(10L);
        inventory.setReservedQuantity(0L);

        UUID inventoryId = UUID.randomUUID();
        inventory.setId(inventoryId);

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(2L);
        reservation.setStatus(
                ReservationStatus.ACTIVE
        );

        ReservationRequest request =
                new ReservationRequest(
                        reservationId,
                        2L
                );

        // First lookup: reservation does not exist.
        // Second lookup: reservation was created successfully.
        when(reservationRepository.findByReservationId(reservationId))
                .thenReturn(
                        Optional.empty(),
                        Optional.of(reservation)
                );

        when(inventoryRepository.findByProductIdForUpdate(productId))
                .thenReturn(Optional.of(inventory));

        when(reservationRepository.insertIfAbsent(
                any(UUID.class),
                eq(reservationId),
                eq(inventoryId),
                eq(2L)
        )).thenReturn(1);

        when(objectMapper.writeValueAsString(
                any(InventoryReservedEvent.class)
        )).thenReturn("{}");

        when(inventoryRepository.save(inventory))
                .thenReturn(inventory);

        when(outboxEventRepository.save(
                any(OutboxEvent.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        InventoryResponse response =
                inventoryService.reserveStock(
                        productId,
                        request
                );

        assertEquals(10L, response.quantity());
        assertEquals(2L, response.reservedQuantity());
        assertEquals(8L, response.availableQuantity());

        verify(reservationRepository, times(2))
                .findByReservationId(reservationId);

        verify(inventoryRepository)
                .findByProductIdForUpdate(productId);

        verify(reservationRepository)
                .insertIfAbsent(
                        any(UUID.class),
                        eq(reservationId),
                        eq(inventoryId),
                        eq(2L)
                );

        verify(inventoryRepository)
                .save(inventory);

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    private OutboxEvent createInventoryReservedEvent(
            InventoryReservation reservation) throws JsonProcessingException {

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        reservation.getReservationId(),
                        reservation.getInventory().getProductId(),
                        reservation.getQuantity(),
                        LocalDateTime.now()
                );

        String payload =
                objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent =
                new OutboxEvent();

        outboxEvent.setAggregateType(
                "InventoryReservation"
        );

        outboxEvent.setAggregateId(
                reservation.getReservationId()
        );

        outboxEvent.setEventType(
                InventoryEventType.INVENTORY_RESERVED.name()
        );

        outboxEvent.setPayload(payload);

        outboxEvent.setStatus(
                OutboxEventStatus.PENDING
        );

        return outboxEvent;
    }

    @Test
    void shouldRejectDuplicateInventory() {

        CreateInventoryRequest request =
                new CreateInventoryRequest(
                        productId,
                        100L
                );

        Inventory existingInventory =
                createInventory(50L, 0L);

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(existingInventory));

        assertThrows(
                InvalidInventoryOperationException.class,
                () -> inventoryService.createInventory(request)
        );

        verify(inventoryRepository, never())
                .save(any(Inventory.class));
    }

    // ---------------------------------------------------------
    // ADD STOCK
    // ---------------------------------------------------------

    @Test
    void shouldAddStock() {

        Inventory inventory =
                createInventory(100L, 20L);

        UUID operationId = UUID.randomUUID();

        when(inventoryRepository.findByProductIdForUpdate(productId))
                .thenReturn(Optional.of(inventory));

        when(inventoryStockOperationRepository.insertIfAbsent(
                any(UUID.class),
                eq(operationId),
                eq(inventory.getId()),
                eq("ADD"),
                eq(50L)
        )).thenReturn(1);

        when(inventoryRepository.save(inventory))
                .thenReturn(inventory);

        InventoryResponse response =
                inventoryService.addStock(
                        productId,
                        new StockAdjustmentRequest(
                                operationId,
                                50L
                        )
                );

        assertEquals(
                150L,
                response.quantity()
        );

        assertEquals(
                20L,
                response.reservedQuantity()
        );

        assertEquals(
                130L,
                response.availableQuantity()
        );

        verify(inventoryRepository)
                .save(inventory);
    }

    @Test
    void shouldRemoveStockFromAvailableQuantity() {

        Inventory inventory =
                createInventory(100L, 20L);

        UUID operationId = UUID.randomUUID();

        when(inventoryRepository.findByProductIdForUpdate(productId))
                .thenReturn(Optional.of(inventory));

        when(inventoryStockOperationRepository.insertIfAbsent(
                any(UUID.class),
                eq(operationId),
                eq(inventory.getId()),
                eq("REMOVE"),
                eq(30L)
        )).thenReturn(1);

        when(inventoryRepository.save(inventory))
                .thenReturn(inventory);

        InventoryResponse response =
                inventoryService.removeStock(
                        productId,
                        new StockAdjustmentRequest(
                                operationId,
                                30L
                        )
                );

        assertEquals(
                70L,
                response.quantity()
        );

        assertEquals(
                20L,
                response.reservedQuantity()
        );

        assertEquals(
                50L,
                response.availableQuantity()
        );

        verify(inventoryRepository)
                .save(inventory);
    }


    // ---------------------------------------------------------
    // RESERVE STOCK
    // ---------------------------------------------------------

    @Test
    void shouldReserveStock() throws JsonProcessingException {

        UUID reservationId =
                UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 20L);

        InventoryReservation reservation =
                new InventoryReservation();

        reservation.setReservationId(reservationId);
        reservation.setInventory(inventory);
        reservation.setQuantity(30L);
        reservation.setStatus(
                ReservationStatus.ACTIVE
        );

        // 1st lookup: reservation does not exist
        // 2nd lookup: reservation was created successfully
        when(
                reservationRepository.findByReservationId(reservationId)
        ).thenReturn(
                Optional.empty(),
                Optional.of(reservation)
        );

        when(
                inventoryRepository.findByProductIdForUpdate(productId)
        ).thenReturn(Optional.of(inventory));

        when(
                reservationRepository.insertIfAbsent(
                        any(UUID.class),
                        eq(reservationId),
                        eq(inventory.getId()),
                        eq(30L)
                )
        ).thenReturn(1);

        when(
                inventoryRepository.save(inventory)
        ).thenReturn(inventory);

        when(
                objectMapper.writeValueAsString(
                        any(InventoryReservedEvent.class)
                )
        ).thenReturn("{}");

        when(
                outboxEventRepository.save(
                        any(OutboxEvent.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        InventoryResponse response =
                inventoryService.reserveStock(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                30L
                        )
                );

        assertEquals(100L, response.quantity());
        assertEquals(50L, response.reservedQuantity());
        assertEquals(50L, response.availableQuantity());

        verify(reservationRepository, times(2))
                .findByReservationId(reservationId);

        verify(inventoryRepository)
                .findByProductIdForUpdate(productId);

        verify(reservationRepository)
                .insertIfAbsent(
                        any(UUID.class),
                        eq(reservationId),
                        eq(inventory.getId()),
                        eq(30L)
                );

        verify(inventoryRepository)
                .save(inventory);

        verify(outboxEventRepository)
                .save(any(OutboxEvent.class));
    }

    @Test
    void shouldReturnExistingInventoryForDuplicateReservation() {

        UUID reservationId =
                UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 30L);

        InventoryReservation existingReservation =
                createReservation(
                        reservationId,
                        inventory,
                        30L,
                        ReservationStatus.ACTIVE
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(
                Optional.of(existingReservation)
        );

        InventoryResponse response =
                inventoryService.reserveStock(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                30L
                        )
                );

        assertEquals(
                100L,
                response.quantity()
        );

        assertEquals(
                30L,
                response.reservedQuantity()
        );

        assertEquals(
                70L,
                response.availableQuantity()
        );

        verify(
                reservationRepository
        ).findByReservationId(reservationId);

        verify(
                inventoryRepository,
                never()
        ).findByProductId(any(UUID.class));

        verify(
                reservationRepository,
                never()
        ).insertIfAbsent(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                anyLong()
        );

        verify(
                inventoryRepository,
                never()
        ).save(any(Inventory.class));
    }

    @Test
    void shouldRejectReservationForDifferentProduct() {

        UUID reservationId = UUID.randomUUID();
        UUID anotherProductId = UUID.randomUUID();

        Inventory existingReservationInventory =
                createInventory(
                        UUID.randomUUID(),
                        productId,
                        100L,
                        30L
                );

        InventoryReservation existingReservation =
                createReservation(
                        reservationId,
                        existingReservationInventory,
                        30L,
                        ReservationStatus.ACTIVE
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.of(existingReservation));

        assertThrows(
                InvalidInventoryOperationException.class,
                () -> inventoryService.reserveStock(
                        anotherProductId,
                        new ReservationRequest(
                                reservationId,
                                30L
                        )
                )
        );

        verify(reservationRepository)
                .findByReservationId(reservationId);

        verify(inventoryRepository, never())
                .findByProductId(any(UUID.class));

        verify(reservationRepository, never())
                .insertIfAbsent(
                        any(UUID.class),
                        any(UUID.class),
                        any(UUID.class),
                        anyLong()
                );

        verify(inventoryRepository, never())
                .save(any(Inventory.class));
    }

    @Test
    void shouldRejectReservationWhenInsufficientStock() {

        UUID reservationId = UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 80L);

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.empty());

        when(
                inventoryRepository.findByProductIdForUpdate(productId)
        ).thenReturn(Optional.of(inventory));

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.reserveStock(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                30L
                        )
                )
        );

        assertEquals(100L, inventory.getQuantity());
        assertEquals(80L, inventory.getReservedQuantity());

        verify(reservationRepository)
                .findByReservationId(reservationId);

        verify(inventoryRepository)
                .findByProductIdForUpdate(productId);

        verify(
                reservationRepository,
                never()
        ).insertIfAbsent(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                anyLong()
        );

        verify(
                inventoryRepository,
                never()
        ).save(any(Inventory.class));
    }

    // ---------------------------------------------------------
    // RELEASE RESERVATION
    // ---------------------------------------------------------

    @Test
    void shouldReleaseReservation() {

        UUID reservationId =
                UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 40L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        15L,
                        ReservationStatus.ACTIVE
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.of(reservation));

        when(inventoryRepository.save(inventory))
                .thenReturn(inventory);

        when(
                reservationRepository.markReleasedIfActive(
                        reservationId
                )
        ).thenReturn(1);

        InventoryResponse response =
                inventoryService.releaseReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                15L
                        )
                );

        assertEquals(
                100L,
                response.quantity()
        );

        assertEquals(
                25L,
                response.reservedQuantity()
        );

        assertEquals(
                75L,
                response.availableQuantity()
        );

        verify(inventoryRepository)
                .save(inventory);

        verify(reservationRepository)
                .markReleasedIfActive(reservationId);

        verify(
                reservationRepository,
                never()
        ).save(any(InventoryReservation.class));
    }

    @Test
    void shouldRejectReleasingCommittedReservation() {

        UUID reservationId = UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 0L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        20L,
                        ReservationStatus.COMMITTED
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.of(reservation));

        assertThrows(
                InvalidInventoryOperationException.class,
                () -> inventoryService.releaseReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                20L
                        )
                )
        );

        assertEquals(100L, inventory.getQuantity());
        assertEquals(0L, inventory.getReservedQuantity());

        verify(inventoryRepository, never())
                .save(any(Inventory.class));

        verify(reservationRepository, never())
                .save(any(InventoryReservation.class));
    }

    // ---------------------------------------------------------
    // COMMIT RESERVATION
    // ---------------------------------------------------------

    @Test
    void shouldCommitReservation() {

        UUID reservationId =
                UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 30L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        10L,
                        ReservationStatus.ACTIVE
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.of(reservation));

        when(inventoryRepository.save(inventory))
                .thenReturn(inventory);

        // NEW: mock the atomic ACTIVE -> COMMITTED transition
        when(
                reservationRepository.markCommittedIfActive(
                        reservationId
                )
        ).thenReturn(1);

        InventoryResponse response =
                inventoryService.commitReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                10L
                        )
                );

        assertEquals(
                90L,
                response.quantity()
        );

        assertEquals(
                20L,
                response.reservedQuantity()
        );

        assertEquals(
                70L,
                response.availableQuantity()
        );

        verify(inventoryRepository)
                .save(inventory);

        verify(reservationRepository)
                .markCommittedIfActive(reservationId);

        verify(
                reservationRepository,
                never()
        ).save(any(InventoryReservation.class));
    }

    @Test
    void shouldRejectCommittingReleasedReservation() {

        UUID reservationId = UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 0L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        20L,
                        ReservationStatus.RELEASED
                );

        when(
                reservationRepository.findByReservationId(
                        reservationId
                )
        ).thenReturn(Optional.of(reservation));

        assertThrows(
                InvalidInventoryOperationException.class,
                () -> inventoryService.commitReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                20L
                        )
                )
        );

        assertEquals(100L, inventory.getQuantity());
        assertEquals(0L, inventory.getReservedQuantity());

        verify(inventoryRepository, never())
                .save(any(Inventory.class));

        verify(reservationRepository, never())
                .save(any(InventoryReservation.class));
    }
    // ---------------------------------------------------------
    // GET INVENTORY
    // ---------------------------------------------------------

    @Test
    void shouldGetInventory() {

        Inventory inventory =
                createInventory(100L, 25L);

        when(
                inventoryRepository.findByProductId(productId)
        ).thenReturn(Optional.of(inventory));

        InventoryResponse response =
                inventoryService.getInventory(productId);

        assertEquals(
                inventoryId,
                response.id()
        );

        assertEquals(
                productId,
                response.productId()
        );

        assertEquals(
                100L,
                response.quantity()
        );

        assertEquals(
                25L,
                response.reservedQuantity()
        );

        assertEquals(
                75L,
                response.availableQuantity()
        );

        assertEquals(
                0L,
                response.version()
        );

        verify(inventoryRepository)
                .findByProductId(productId);
    }

    // ---------------------------------------------------------
    // INVENTORY NOT FOUND
    // ---------------------------------------------------------

    @Test
    void shouldThrowWhenInventoryDoesNotExist() {

        when(
                inventoryRepository.findByProductId(productId)
        ).thenReturn(Optional.empty());

        assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventory(productId)
        );
    }

    @Test
    void releasingAlreadyReleasedReservationShouldBeIdempotent() {

        UUID reservationId = UUID.randomUUID();

        Inventory inventory =
                createInventory(100L, 20L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        20L,
                        ReservationStatus.RELEASED
                );

        when(reservationRepository.findByReservationId(
                reservationId
        )).thenReturn(Optional.of(reservation));

        InventoryResponse response =
                inventoryService.releaseReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                20L
                        )
                );

        assertEquals(
                100L,
                response.quantity()
        );

        assertEquals(
                20L,
                response.reservedQuantity()
        );

        verify(inventoryRepository, never())
                .save(any());

        verify(reservationRepository, never())
                .save(any());
    }

    @Test
    void committingAlreadyCommittedReservationShouldBeIdempotent() {

        UUID reservationId = UUID.randomUUID();

        Inventory inventory =
                createInventory(80L, 0L);

        InventoryReservation reservation =
                createReservation(
                        reservationId,
                        inventory,
                        20L,
                        ReservationStatus.COMMITTED
                );

        when(reservationRepository.findByReservationId(
                reservationId
        )).thenReturn(Optional.of(reservation));

        InventoryResponse response =
                inventoryService.commitReservation(
                        productId,
                        new ReservationRequest(
                                reservationId,
                                20L
                        )
                );

        assertEquals(
                80L,
                response.quantity()
        );

        assertEquals(
                0L,
                response.reservedQuantity()
        );

        verify(inventoryRepository, never())
                .save(any());

        verify(reservationRepository, never())
                .save(any());
    }
}

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryStockOperationRepository stockOperationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final InventoryMetrics inventoryMetrics;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryStockOperationRepository stockOperationRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            InventoryMetrics inventoryMetrics) {

        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.stockOperationRepository = stockOperationRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.inventoryMetrics = inventoryMetrics;
    }

    @Transactional
    public InventoryResponse createInventory(
            CreateInventoryRequest request) {

        if (inventoryRepository
                .findByProductId(request.productId())
                .isPresent()) {

            throw new InvalidInventoryOperationException(
                    "Inventory already exists for product: "
                            + request.productId()
            );
        }

        Inventory inventory = new Inventory();

        inventory.setProductId(request.productId());
        inventory.setQuantity(request.quantity());
        inventory.setReservedQuantity(0L);

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return toResponse(savedInventory);
    }

    @Transactional
    public InventoryResponse addStock(
            UUID productId,
            StockAdjustmentRequest request) {

        /*
         * Lock the inventory row.
         *
         * Stock mutations for the same product are therefore
         * serialized at the database level.
         */
        Inventory inventory =
                inventoryRepository
                        .findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new InventoryNotFoundException(productId));

        /*
         * Check idempotency after acquiring the lock.
         */
        int inserted =
                stockOperationRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        request.operationId(),
                        inventory.getId(),
                        StockOperationType.ADD.name(),
                        request.quantity());

        /*
         * Operation already exists.
         *
         * Treat this as an idempotent retry.
         *
         * IMPORTANT:
         * We do NOT increment the stock adjustment metric here
         * because no new stock adjustment happened.
         */
        if (inserted == 0) {

            InventoryStockOperation existingOperation =
                    stockOperationRepository
                            .findByOperationId(
                                    request.operationId())
                            .orElseThrow(() ->
                                    new InvalidInventoryOperationException(
                                            "Stock operation could not be resolved: "
                                                    + request.operationId()));

            validateStockOperation(
                    existingOperation,
                    productId,
                    request,
                    StockOperationType.ADD);

            return toResponse(
                    existingOperation.getInventory());
        }

        /*
         * Perform the actual stock increase while
         * holding the database lock.
         */
        inventory.setQuantity(
                inventory.getQuantity()
                        + request.quantity());

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        /*
         * Record only a genuine successful stock adjustment.
         */
        inventoryMetrics.stockAdjustment();

        return toResponse(savedInventory);
    }

    @Transactional
    public InventoryResponse removeStock(
            UUID productId,
            StockAdjustmentRequest request) {

        /*
         * Lock inventory before checking available stock.
         */
        Inventory inventory =
                inventoryRepository
                        .findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new InventoryNotFoundException(productId));

        /*
         * Idempotency check.
         */
        int inserted =
                stockOperationRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        request.operationId(),
                        inventory.getId(),
                        StockOperationType.REMOVE.name(),
                        request.quantity());

        /*
         * Operation already exists.
         *
         * Treat as idempotent retry.
         *
         * No metric increment because no new
         * stock adjustment happened.
         */
        if (inserted == 0) {

            InventoryStockOperation existingOperation =
                    stockOperationRepository
                            .findByOperationId(
                                    request.operationId())
                            .orElseThrow(() ->
                                    new InvalidInventoryOperationException(
                                            "Stock operation could not be resolved: "
                                                    + request.operationId()));

            validateStockOperation(
                    existingOperation,
                    productId,
                    request,
                    StockOperationType.REMOVE);

            return toResponse(
                    existingOperation.getInventory());
        }

        /*
         * IMPORTANT:
         *
         * This check happens while the inventory row
         * is locked.
         */
        long availableQuantity =
                calculateAvailableQuantity(inventory);

        if (request.quantity() > availableQuantity) {

            /*
             * Record the rejected business operation.
             *
             * The surrounding transaction will roll back,
             * including the idempotency operation row.
             */
            inventoryMetrics.insufficientStock();

            throw new InsufficientStockException(
                    request.quantity(),
                    availableQuantity);
        }

        inventory.setQuantity(
                inventory.getQuantity()
                        - request.quantity());

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        /*
         * Genuine successful stock removal.
         */
        inventoryMetrics.stockAdjustment();

        return toResponse(savedInventory);
    }

    private void validateStockOperation(
            InventoryStockOperation operation,
            UUID productId,
            StockAdjustmentRequest request,
            StockOperationType expectedType) {

        if (!operation.getInventory()
                .getProductId()
                .equals(productId)) {

            throw new InvalidInventoryOperationException(
                    "Operation does not belong to this product"
            );
        }

        if (operation.getOperationType()
                != expectedType) {

            throw new InvalidInventoryOperationException(
                    "Operation already exists with type: "
                            + operation.getOperationType()
            );
        }

        if (!operation.getQuantity()
                .equals(request.quantity())) {

            throw new InvalidInventoryOperationException(
                    "Operation already exists with quantity: "
                            + operation.getQuantity()
            );
        }
    }

    @Transactional
    public InventoryResponse reserveStock(
            UUID productId,
            ReservationRequest request) {

        /*
         * STEP 1:
         *
         * First check whether this reservation already exists.
         *
         * This allows retries to be handled idempotently without
         * unnecessarily locking the inventory row.
         */
        var existingReservation =
                reservationRepository.findByReservationId(
                        request.reservationId());

        if (existingReservation.isPresent()) {

            InventoryReservation reservation =
                    existingReservation.get();

            validateReservationProduct(
                    reservation,
                    productId);

            validateReservationQuantity(
                    reservation,
                    request.quantity());

            validateReservationCanBeReused(
                    reservation);

            /*
             * Idempotent retry.
             *
             * No reservation-created metric because no
             * new reservation was created.
             */
            return toResponse(
                    reservation.getInventory());
        }

        /*
         * STEP 2:
         *
         * Reservation does not exist yet.
         *
         * Now acquire the pessimistic database lock.
         *
         * This serializes concurrent stock reservations
         * for the same product.
         */
        Inventory inventory =
                inventoryRepository
                        .findByProductIdForUpdate(productId)
                        .orElseThrow(() ->
                                new InventoryNotFoundException(productId));

        /*
         * STEP 3:
         *
         * Calculate availability while holding the
         * inventory row lock.
         */
        long availableQuantity =
                calculateAvailableQuantity(inventory);

        if (request.quantity() > availableQuantity) {

            inventoryMetrics.insufficientStock();

            throw new InsufficientStockException(
                    request.quantity(),
                    availableQuantity);
        }

        /*
         * STEP 4:
         *
         * Attempt to create the reservation.
         *
         * reservation_id has a UNIQUE constraint in the database.
         *
         * Therefore concurrent requests using the same reservationId
         * cannot create duplicate reservation records.
         */
        UUID reservationEntityId =
                UUID.randomUUID();

        int inserted =
                reservationRepository.insertIfAbsent(
                        reservationEntityId,
                        request.reservationId(),
                        inventory.getId(),
                        request.quantity());

        /*
         * STEP 5:
         *
         * Another request may have created the same reservation
         * between our initial lookup and this INSERT.
         *
         * ON CONFLICT DO NOTHING therefore returns 0.
         *
         * Treat that as an idempotent retry.
         */
        if (inserted == 0) {

            InventoryReservation reservation =
                    reservationRepository
                            .findByReservationId(
                                    request.reservationId())
                            .orElseThrow(() ->
                                    new InvalidInventoryOperationException(
                                            "Reservation could not be resolved: "
                                                    + request.reservationId()));

            validateReservationProduct(
                    reservation,
                    productId);

            validateReservationQuantity(
                    reservation,
                    request.quantity());

            validateReservationCanBeReused(
                    reservation);

            return toResponse(
                    reservation.getInventory());
        }

        /*
         * STEP 6:
         *
         * Reservation was successfully created.
         *
         * Update the aggregate reserved counter.
         */
        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        + request.quantity());

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        /*
         * STEP 7:
         *
         * Create the Outbox event in the SAME transaction.
         *
         * Reservation INSERT
         * Inventory UPDATE
         * Outbox INSERT
         *
         * all commit or roll back together.
         */
        InventoryReservation reservation =
                reservationRepository
                        .findByReservationId(
                                request.reservationId())
                        .orElseThrow(() ->
                                new InvalidInventoryOperationException(
                                        "Created reservation could not be loaded"));

        OutboxEvent outboxEvent =
                createInventoryReservedEvent(
                        reservation);

        outboxEventRepository.save(outboxEvent);

        /*
         * Record only a genuinely created reservation.
         */
        inventoryMetrics.reservationCreated();

        return toResponse(savedInventory);
    }

    /*
     * Test-only transaction rollback method.
     *
     * This intentionally fails after multiple database writes
     * to prove transaction atomicity.
     */
    @Transactional
    public InventoryResponse reserveStockAndFail(
            UUID productId,
            ReservationRequest request) {

        Inventory inventory =
                findInventory(productId);

        long availableQuantity =
                calculateAvailableQuantity(inventory);

        if (request.quantity() > availableQuantity) {

            throw new InsufficientStockException(
                    request.quantity(),
                    availableQuantity
            );
        }

        UUID reservationEntityId =
                UUID.randomUUID();

        int inserted =
                reservationRepository.insertIfAbsent(
                        reservationEntityId,
                        request.reservationId(),
                        inventory.getId(),
                        request.quantity()
                );

        if (inserted == 0) {

            throw new InvalidInventoryOperationException(
                    "Reservation already exists"
            );
        }

        InventoryReservation reservation =
                reservationRepository
                        .findByReservationId(
                                request.reservationId()
                        )
                        .orElseThrow(
                                () -> new InvalidInventoryOperationException(
                                        "Reservation could not be loaded"
                                )
                        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        + request.quantity()
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        OutboxEvent outboxEvent =
                createInventoryReservedEvent(
                        reservation
                );

        outboxEventRepository.save(outboxEvent);

        /*
         * Deliberate failure.
         *
         * This exists only to prove that:
         *
         * reservation INSERT
         * inventory UPDATE
         * outbox INSERT
         *
         * all roll back together.
         */
        throw new RuntimeException(
                "Intentional failure for transaction rollback test"
        );
    }

    /*
     * Test/demo method for pessimistic locking behavior.
     */
    @Transactional
    public InventoryResponse reserveStockPessimistically(
            UUID productId,
            ReservationRequest request) {

        Inventory inventory =
                inventoryRepository
                        .findByProductIdForUpdate(productId)
                        .orElseThrow(
                                () -> new InventoryNotFoundException(productId)
                        );

        long availableQuantity =
                calculateAvailableQuantity(inventory);

        if (request.quantity() > availableQuantity) {

            inventoryMetrics.insufficientStock();

            throw new InsufficientStockException(
                    request.quantity(),
                    availableQuantity
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        + request.quantity()
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return toResponse(savedInventory);
    }

    private void validateReservationQuantity(
            InventoryReservation reservation,
            Long requestedQuantity) {

        if (!reservation.getQuantity()
                .equals(requestedQuantity)) {

            throw new InvalidInventoryOperationException(
                    "Reservation already exists with quantity: "
                            + reservation.getQuantity()
            );
        }
    }

    private void validateReservationCanBeReused(
            InventoryReservation reservation) {

        if (reservation.getStatus()
                != ReservationStatus.ACTIVE) {

            throw new InvalidInventoryOperationException(
                    "Reservation already exists with status: "
                            + reservation.getStatus()
            );
        }
    }

    @Transactional
    public InventoryResponse releaseReservation(
            UUID productId,
            ReservationRequest request) {

        InventoryReservation reservation =
                findReservation(request.reservationId());

        validateReservationProduct(
                reservation,
                productId
        );

        validateReservationQuantity(
                reservation,
                request.quantity()
        );

        /*
         * Already released = idempotent success.
         */
        if (reservation.getStatus()
                == ReservationStatus.RELEASED) {

            return toResponse(
                    reservation.getInventory()
            );
        }

        /*
         * Already committed = invalid transition.
         */
        if (reservation.getStatus()
                == ReservationStatus.COMMITTED) {

            throw new InvalidInventoryOperationException(
                    "Committed reservation cannot be released"
            );
        }

        /*
         * Atomically claim:
         *
         * ACTIVE -> RELEASED
         */
        int updated =
                reservationRepository.markReleasedIfActive(
                        reservation.getReservationId()
                );

        /*
         * Another identical request may have completed
         * the release while we were waiting.
         */
        if (updated == 0) {

            InventoryReservation latestReservation =
                    findReservation(
                            request.reservationId()
                    );

            validateReservationProduct(
                    latestReservation,
                    productId
            );

            validateReservationQuantity(
                    latestReservation,
                    request.quantity()
            );

            if (latestReservation.getStatus()
                    == ReservationStatus.RELEASED) {

                return toResponse(
                        latestReservation.getInventory()
                );
            }

            if (latestReservation.getStatus()
                    == ReservationStatus.COMMITTED) {

                throw new InvalidInventoryOperationException(
                        "Committed reservation cannot be released"
                );
            }

            throw new InvalidInventoryOperationException(
                    "Reservation state could not be completed: "
                            + request.reservationId()
            );
        }

        /*
         * We successfully transitioned:
         *
         * ACTIVE -> RELEASED
         *
         * Now release the reserved quantity.
         */
        reservation =
                findReservation(
                        request.reservationId()
                );

        Inventory inventory =
                reservation.getInventory();

        validateReservationInventoryState(
                inventory,
                reservation
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        - reservation.getQuantity()
        );

        inventoryRepository.save(inventory);

        /*
         * Only the actual ACTIVE -> RELEASED transition
         * counts as a release.
         */
        inventoryMetrics.reservationReleased();

        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse commitReservation(
            UUID productId,
            ReservationRequest request) {

        InventoryReservation reservation =
                findReservation(request.reservationId());

        validateReservationProduct(
                reservation,
                productId
        );

        validateReservationQuantity(
                reservation,
                request.quantity()
        );

        /*
         * Already committed = idempotent success.
         */
        if (reservation.getStatus()
                == ReservationStatus.COMMITTED) {

            return toResponse(
                    reservation.getInventory()
            );
        }

        /*
         * Released reservation can never be committed.
         */
        if (reservation.getStatus()
                == ReservationStatus.RELEASED) {

            throw new InvalidInventoryOperationException(
                    "Released reservation cannot be committed"
            );
        }

        /*
         * Atomically claim:
         *
         * ACTIVE -> COMMITTED
         *
         * Only one concurrent request can get updated = 1.
         */
        int updated =
                reservationRepository.markCommittedIfActive(
                        reservation.getReservationId()
                );

        /*
         * Another identical request may have completed
         * the transition while we were waiting.
         */
        if (updated == 0) {

            InventoryReservation latestReservation =
                    findReservation(
                            request.reservationId()
                    );

            validateReservationProduct(
                    latestReservation,
                    productId
            );

            validateReservationQuantity(
                    latestReservation,
                    request.quantity()
            );

            if (latestReservation.getStatus()
                    == ReservationStatus.COMMITTED) {

                return toResponse(
                        latestReservation.getInventory()
                );
            }

            if (latestReservation.getStatus()
                    == ReservationStatus.RELEASED) {

                throw new InvalidInventoryOperationException(
                        "Released reservation cannot be committed"
                );
            }

            throw new InvalidInventoryOperationException(
                    "Reservation state could not be completed: "
                            + request.reservationId()
            );
        }

        /*
         * We successfully transitioned:
         *
         * ACTIVE -> COMMITTED
         *
         * Now we are responsible for consuming the reserved stock.
         */
        reservation =
                findReservation(
                        request.reservationId()
                );

        Inventory inventory =
                reservation.getInventory();

        validateReservationInventoryState(
                inventory,
                reservation
        );

        inventory.setQuantity(
                inventory.getQuantity()
                        - reservation.getQuantity()
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        - reservation.getQuantity()
        );

        inventoryRepository.save(inventory);

        /*
         * Only the actual ACTIVE -> COMMITTED transition
         * counts as a committed reservation.
         */
        inventoryMetrics.reservationCommitted();

        return toResponse(inventory);
    }

    private void validateReservationInventoryState(
            Inventory inventory,
            InventoryReservation reservation) {

        if (reservation.getQuantity()
                > inventory.getReservedQuantity()) {

            throw new InvalidInventoryOperationException(
                    "Reserved quantity is inconsistent for reservation: "
                            + reservation.getReservationId()
            );
        }
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(
            UUID productId) {

        Inventory inventory =
                findInventory(productId);

        return toResponse(inventory);
    }

    private Inventory findInventory(
            UUID productId) {

        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(
                        () -> new InventoryNotFoundException(productId)
                );
    }

    private InventoryReservation findReservation(
            UUID reservationId) {

        return reservationRepository
                .findByReservationId(reservationId)
                .orElseThrow(
                        () -> new InvalidInventoryOperationException(
                                "Reservation not found: "
                                        + reservationId
                        )
                );
    }

    private void validateReservationProduct(
            InventoryReservation reservation,
            UUID productId) {

        if (!reservation.getInventory()
                .getProductId()
                .equals(productId)) {

            throw new InvalidInventoryOperationException(
                    "Reservation does not belong to this product"
            );
        }
    }

    private long calculateAvailableQuantity(
            Inventory inventory) {

        return inventory.getQuantity()
                - inventory.getReservedQuantity();
    }

    private InventoryResponse toResponse(
            Inventory inventory) {

        long availableQuantity =
                calculateAvailableQuantity(inventory);

        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                availableQuantity,
                inventory.getVersion(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    private OutboxEvent createInventoryReservedEvent(
            InventoryReservation reservation) {

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        reservation.getReservationId(),
                        reservation.getInventory().getProductId(),
                        reservation.getQuantity(),
                        LocalDateTime.now()
                );

        final String payload;

        try {

            payload =
                    objectMapper.writeValueAsString(event);

        } catch (JsonProcessingException e) {

            throw new InvalidInventoryOperationException(
                    "Failed to serialize inventory reserved event",
                    e
            );
        }

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

    /*
     * Test-only transaction rollback method.
     *
     * This intentionally fails after the operation row
     * and inventory update have been written.
     */
    @Transactional
    public InventoryResponse addStockAndFail(
            UUID productId,
            StockAdjustmentRequest request) {

        Inventory inventory =
                findInventory(productId);

        UUID operationEntityId =
                UUID.randomUUID();

        int inserted =
                stockOperationRepository.insertIfAbsent(
                        operationEntityId,
                        request.operationId(),
                        inventory.getId(),
                        "ADD",
                        request.quantity()
                );

        if (inserted == 0) {

            throw new InvalidInventoryOperationException(
                    "Operation already exists"
            );
        }

        inventory.setQuantity(
                inventory.getQuantity()
                        + request.quantity()
        );

        inventoryRepository.save(inventory);

        /*
         * Deliberate failure AFTER both writes.
         *
         * The transaction rolls back, therefore we deliberately
         * do NOT increment the successful stock metric here.
         */
        throw new RuntimeException(
                "Simulated failure"
        );
    }
}
package com.clothflow.shipping.service;

import com.clothflow.shipping.config.ShippingMetrics;
import com.clothflow.shipping.dto.request.ShipmentItemRequest;
import com.clothflow.shipping.dto.response.ShipmentResponse;
import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentItem;
import com.clothflow.shipping.entity.ShipmentStatus;
import com.clothflow.shipping.event.EventEnvelope;
import com.clothflow.shipping.event.OrderConfirmedEvent;
import com.clothflow.shipping.event.OrderConfirmedItem;
import com.clothflow.shipping.event.ShippingEventType;
import com.clothflow.shipping.exception.ShipmentAlreadyExistsException;
import com.clothflow.shipping.exception.ShipmentForOrderNotFoundException;
import com.clothflow.shipping.exception.ShipmentNotFoundException;
import com.clothflow.shipping.mapper.ShipmentMapper;
import com.clothflow.shipping.outbox.ShippingOutboxService;
import com.clothflow.shipping.provider.ShippingLabel;
import com.clothflow.shipping.provider.ShippingProvider;
import com.clothflow.shipping.provider.ShippingProviderException;
import com.clothflow.shipping.repository.ShipmentRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ShipmentService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ShipmentService.class
            );

    private final ShipmentRepository shipmentRepository;

    private final ProcessedEventService processedEventService;

    private final ShipmentMapper shipmentMapper;

    private final ShippingOutboxService shippingOutboxService;

    private final ShippingProvider shippingProvider;

    private final ShipmentPersistenceService
            shipmentPersistenceService;

    private final ShippingMetrics shippingMetrics;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            ProcessedEventService processedEventService,
            ShipmentMapper shipmentMapper,
            ShippingOutboxService shippingOutboxService,
            ShippingProvider shippingProvider,
            ShipmentPersistenceService shipmentPersistenceService,
            ShippingMetrics shippingMetrics
    ) {
        this.shipmentRepository =
                shipmentRepository;

        this.processedEventService =
                processedEventService;

        this.shipmentMapper =
                shipmentMapper;

        this.shippingOutboxService =
                shippingOutboxService;

        this.shippingProvider =
                shippingProvider;

        this.shipmentPersistenceService =
                shipmentPersistenceService;

        this.shippingMetrics =
                shippingMetrics;
    }

    /*
     * ============================================================
     * REST / MANUAL SHIPMENT CREATION
     * ============================================================
     */

    @Transactional
    public Shipment createShipment(
            UUID orderId,
            UUID customerId,
            String recipientName,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            List<ShipmentItemRequest> items
    ) {

        if (shipmentRepository.existsByOrderId(orderId)) {

            throw new ShipmentAlreadyExistsException(
                    orderId
            );
        }

        Shipment shipment =
                new Shipment(
                        orderId,
                        customerId,
                        recipientName,
                        addressLine1,
                        addressLine2,
                        city,
                        state,
                        postalCode,
                        country
                );

        for (ShipmentItemRequest item : items) {

            shipment.addItem(
                    new ShipmentItem(
                            item.productId(),
                            item.productName(),
                            item.quantity()
                    )
            );
        }

        Shipment savedShipment =
                shipmentRepository.saveAndFlush(
                        shipment
                );

        /*
         * Count only after the shipment has actually been
         * persisted successfully.
         */
        shippingMetrics.shipmentCreated();

        return savedShipment;
    }

    /*
     * ============================================================
     * READ OPERATIONS
     * ============================================================
     */

    @Transactional(readOnly = true)
    public Shipment getShipment(
            UUID shipmentId
    ) {

        return shipmentRepository
                .findById(shipmentId)
                .orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );
    }

    public Shipment getShipment(
            UUID shipmentId,
            UUID customerId
    ) {

        return shipmentRepository
                .findByIdAndCustomerId(
                        shipmentId,
                        customerId
                )
                .orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );
    }

    public Shipment getShipmentByOrderId(
            UUID orderId,
            UUID customerId
    ) {

        return shipmentRepository
                .findByOrderIdAndCustomerId(
                        orderId,
                        customerId
                )
                .orElseThrow(
                        () ->
                                new ShipmentForOrderNotFoundException(
                                        orderId
                                )
                );
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentByOrderId(
            UUID orderId
    ) {

        return shipmentRepository
                .findByOrderId(orderId)
                .orElseThrow(
                        () ->
                                new ShipmentForOrderNotFoundException(
                                        orderId
                                )
                );
    }

    /*
     * ============================================================
     * SHIPMENT LIFECYCLE
     * ============================================================
     */

    @Transactional
    public ShipmentResponse startProcessing(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.startProcessing();

        Shipment savedShipment =
                shipmentRepository.save(
                        shipment
                );

        /*
         * PROCESSING is an internal state.
         *
         * No lifecycle metric is emitted because PROCESSING
         * represents an intermediate state rather than a
         * completed shipment milestone.
         */
        return shipmentMapper.toResponse(
                savedShipment
        );
    }

    /*
     * ============================================================
     * COMMON STATE + OUTBOX PERSISTENCE
     * ============================================================
     */

    private ShipmentResponse saveWithEvent(
            Shipment shipment,
            ShippingEventType eventType
    ) {

        /*
         * Persist shipment state first.
         */
        Shipment savedShipment =
                shipmentRepository.save(
                        shipment
                );

        /*
         * Create the lifecycle event inside the same
         * transaction.
         */
        shippingOutboxService
                .createShipmentLifecycleEvent(
                        savedShipment,
                        eventType.name()
                );

        /*
         * Only after the state + outbox operation has
         * succeeded do we record the lifecycle metric.
         */
        recordLifecycleMetric(
                eventType
        );

        return shipmentMapper.toResponse(
                savedShipment
        );
    }

    /*
     * Record metrics according to the actual lifecycle
     * event that was successfully persisted.
     */
    private void recordLifecycleMetric(
            ShippingEventType eventType
    ) {

        switch (eventType) {

            case SHIPMENT_SHIPPED ->
                    shippingMetrics.shipmentShipped();

            case SHIPMENT_OUT_FOR_DELIVERY ->
                    shippingMetrics.shipmentOutForDelivery();

            case SHIPMENT_DELIVERED ->
                    shippingMetrics.shipmentDelivered();

            case SHIPMENT_DELIVERY_FAILED ->
                    shippingMetrics.shipmentDeliveryFailed();

            default -> {
                /*
                 * Other event types do not currently represent
                 * lifecycle metrics managed by this service.
                 */
            }
        }
    }

    /*
     * ============================================================
     * PROCESSING → SHIPPED
     * ============================================================
     */

    @Transactional
    public ShipmentResponse ship(
            UUID shipmentId,
            String carrier,
            String trackingNumber
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.markShipped(
                carrier,
                trackingNumber
        );

        return saveWithEvent(
                shipment,
                ShippingEventType.SHIPMENT_SHIPPED
        );
    }

    /*
     * ============================================================
     * SHIPPED → OUT_FOR_DELIVERY
     * ============================================================
     */

    @Transactional
    public ShipmentResponse markOutForDelivery(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.markOutForDelivery();

        return saveWithEvent(
                shipment,
                ShippingEventType.SHIPMENT_OUT_FOR_DELIVERY
        );
    }

    /*
     * ============================================================
     * OUT_FOR_DELIVERY → DELIVERED
     * ============================================================
     */

    @Transactional
    public ShipmentResponse markDelivered(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.markDelivered();

        return saveWithEvent(
                shipment,
                ShippingEventType.SHIPMENT_DELIVERED
        );
    }

    /*
     * ============================================================
     * DELIVERY FAILURE
     * ============================================================
     */

    @Transactional
    public ShipmentResponse markDeliveryFailed(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.markDeliveryFailed();

        return saveWithEvent(
                shipment,
                ShippingEventType.SHIPMENT_DELIVERY_FAILED
        );
    }

    /*
     * ============================================================
     * DELIVERY_FAILED → PROCESSING
     * ============================================================
     */

    @Transactional
    public ShipmentResponse retryDelivery(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.retryDelivery();

        Shipment savedShipment =
                shipmentRepository.save(
                        shipment
                );

        /*
         * Retry is an internal state transition.
         *
         * We don't increment shipment-created/shipped/delivered
         * counters here because no new business milestone
         * occurred.
         */
        return shipmentMapper.toResponse(
                savedShipment
        );
    }

    /*
     * ============================================================
     * CANCEL SHIPMENT
     * ============================================================
     */

    @Transactional
    public ShipmentResponse cancel(
            UUID shipmentId
    ) {

        Shipment shipment =
                shipmentRepository.findById(
                        shipmentId
                ).orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );

        shipment.cancel();

        Shipment savedShipment =
                shipmentRepository.save(
                        shipment
                );

        /*
         * Count only after the cancellation has actually
         * been persisted.
         */
        shippingMetrics.shipmentCancelled();

        return shipmentMapper.toResponse(
                savedShipment
        );
    }

    /*
     * ============================================================
     * ORDER_CONFIRMED → CREATE SHIPMENT
     * ============================================================
     */

    @Transactional
    public boolean handleOrderConfirmed(
            EventEnvelope<?> envelope,
            OrderConfirmedEvent event
    ) {

        /*
         * --------------------------------------------------------
         * 1. INBOX / IDEMPOTENCY
         * --------------------------------------------------------
         */
        boolean newlyProcessed =
                processedEventService.tryMarkProcessed(
                        envelope.eventId(),
                        envelope.eventType(),
                        envelope.aggregateId()
                );

        if (!newlyProcessed) {

            return false;
        }

        /*
         * --------------------------------------------------------
         * 2. VALIDATE AGGREGATE ID
         * --------------------------------------------------------
         */
        if (!envelope.aggregateId()
                .equals(event.orderId())) {

            throw new IllegalStateException(
                    "ORDER_CONFIRMED aggregateId does not "
                            + "match event orderId"
            );
        }

        /*
         * --------------------------------------------------------
         * 3. IDEMPOTENT SHIPMENT PROTECTION
         * --------------------------------------------------------
         */
        if (shipmentRepository
                .findByOrderId(event.orderId())
                .isPresent()) {

            log.warn(
                    "Shipment already exists for order: {}",
                    event.orderId()
            );

            return true;
        }

        /*
         * --------------------------------------------------------
         * 4. CREATE SHIPMENT
         * --------------------------------------------------------
         */
        Shipment shipment =
                new Shipment(
                        event.orderId(),
                        event.customerId(),
                        event.recipientName(),
                        event.addressLine1(),
                        event.addressLine2(),
                        event.city(),
                        event.state(),
                        event.postalCode(),
                        event.country()
                );

        /*
         * --------------------------------------------------------
         * 5. COPY ORDER ITEM SNAPSHOT
         * --------------------------------------------------------
         */
        for (OrderConfirmedItem eventItem :
                event.items()) {

            ShipmentItem shipmentItem =
                    new ShipmentItem(
                            eventItem.productId(),
                            eventItem.productName(),
                            eventItem.quantity()
                    );

            shipment.addItem(
                    shipmentItem
            );
        }

        /*
         * --------------------------------------------------------
         * 6. SAVE SHIPMENT
         * --------------------------------------------------------
         */
        Shipment savedShipment =
                shipmentRepository.save(
                        shipment
                );

        /*
         * --------------------------------------------------------
         * 7. CREATE SHIPMENT_CREATED OUTBOX EVENT
         * --------------------------------------------------------
         */
        shippingOutboxService
                .createShipmentCreatedEvent(
                        savedShipment
                );

        /*
         * The shipment and its outbox event were successfully
         * persisted inside the transaction.
         */
        shippingMetrics.shipmentCreated();

        log.info(
                "Shipment created from ORDER_CONFIRMED: " +
                        "shipmentId={}, orderId={}",
                savedShipment.getId(),
                event.orderId()
        );

        return true;
    }

    /*
     * ============================================================
     * EXTERNAL SHIPPING PROVIDER
     * ============================================================
     */

    public ShipmentResponse shipUsingProvider(
            UUID shipmentId
    ) {

        Shipment shipment =
                getShipmentForShipping(
                        shipmentId
                );

        /*
         * Idempotent success.
         *
         * Shipment was already completed.
         */
        if (shipment.getStatus()
                == ShipmentStatus.SHIPPED) {

            return shipmentMapper.toResponse(
                    shipment
            );
        }

        if (shipment.getStatus()
                != ShipmentStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Shipment can only be shipped from PROCESSING"
            );
        }

        String idempotencyKey =
                "clothflow-shipment-" + shipmentId;

        /*
         * Start measuring only when an actual provider
         * attempt is going to happen.
         */
        Timer.Sample providerTimer =
                shippingMetrics
                        .startProviderProcessingTimer();

        try {

            ShippingLabel shippingLabel;

            try {

                shippingLabel =
                        shippingProvider.createShipment(
                                shipment,
                                idempotencyKey
                        );

            } catch (ShippingProviderException ex) {

                /*
                 * The provider explicitly reported a failure.
                 */
                shippingMetrics.providerFailure();

                throw ex;

            } catch (Exception ex) {

                /*
                 * Unknown provider failure.
                 *
                 * This is still a provider failure from the
                 * Shipping Service's perspective.
                 */
                shippingMetrics.providerFailure();

                throw new ShippingProviderException(
                        "Failed to create shipment with provider",
                        ex
                );
            }

            /*
             * Persist PROCESSING → SHIPPED through the
             * dedicated transactional persistence service.
             */
            ShipmentResponse response =
                    shipmentPersistenceService
                            .persistShippedShipment(
                                    shipmentId,
                                    shippingLabel
                            );

            /*
             * The actual shipment transition is now persisted.
             */
            shippingMetrics.shipmentShipped();

            return response;

        } finally {

            /*
             * Always record provider latency:
             *
             * SUCCESS
             * FAILURE
             * EXCEPTION
             *
             * all contribute to the latency distribution.
             */
            shippingMetrics.recordProviderProcessing(
                    providerTimer
            );
        }
    }

    @Transactional(readOnly = true)
    protected Shipment getShipmentForShipping(
            UUID shipmentId
    ) {

        return shipmentRepository
                .findById(shipmentId)
                .orElseThrow(
                        () ->
                                new ShipmentNotFoundException(
                                        shipmentId
                                )
                );
    }

    @Transactional
    protected ShipmentResponse persistShippedShipment(
            UUID shipmentId,
            ShippingLabel shippingLabel
    ) {

        int updatedRows =
                shipmentRepository.markShippedAtomically(
                        shipmentId,
                        shippingLabel.carrier(),
                        shippingLabel.trackingNumber()
                );

        if (updatedRows == 1) {

            Shipment shipment =
                    shipmentRepository
                            .findById(shipmentId)
                            .orElseThrow(
                                    () ->
                                            new ShipmentNotFoundException(
                                                    shipmentId
                                            )
                            );

            shippingOutboxService
                    .createShipmentLifecycleEvent(
                            shipment,
                            ShippingEventType.SHIPMENT_SHIPPED.name()
                    );

            return shipmentMapper.toResponse(
                    shipment
            );
        }

        /*
         * Another request already completed
         * PROCESSING → SHIPPED.
         */
        Shipment currentShipment =
                shipmentRepository
                        .findById(shipmentId)
                        .orElseThrow(
                                () ->
                                        new ShipmentNotFoundException(
                                                shipmentId
                                        )
                        );

        if (currentShipment.getStatus()
                == ShipmentStatus.SHIPPED) {

            return shipmentMapper.toResponse(
                    currentShipment
            );
        }

        throw new IllegalStateException(
                "Shipment could not be marked as SHIPPED"
        );
    }
}
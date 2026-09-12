package com.clothflow.shipping.service;

import com.clothflow.shipping.config.ShippingMetrics;
import com.clothflow.shipping.dto.response.ShipmentResponse;
import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentStatus;
import com.clothflow.shipping.event.ShippingEventType;
import com.clothflow.shipping.exception.ShipmentNotFoundException;
import com.clothflow.shipping.mapper.ShipmentMapper;
import com.clothflow.shipping.outbox.ShippingOutboxService;
import com.clothflow.shipping.provider.ShippingLabel;
import com.clothflow.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShipmentPersistenceService {

    private final ShipmentRepository shipmentRepository;

    private final ShippingOutboxService shippingOutboxService;

    private final ShipmentMapper shipmentMapper;

    private final ShippingMetrics shippingMetrics;

    public ShipmentPersistenceService(
            ShipmentRepository shipmentRepository,
            ShippingOutboxService shippingOutboxService,
            ShipmentMapper shipmentMapper,
            ShippingMetrics shippingMetrics
    ) {
        this.shipmentRepository =
                shipmentRepository;

        this.shippingOutboxService =
                shippingOutboxService;

        this.shipmentMapper =
                shipmentMapper;

        this.shippingMetrics =
                shippingMetrics;
    }

    @Transactional
    public ShipmentResponse persistShippedShipment(
            UUID shipmentId,
            ShippingLabel shippingLabel
    ) {

        /*
         * Atomically attempt:
         *
         * PROCESSING -> SHIPPED
         *
         * The repository method is responsible for ensuring
         * that only one concurrent request can perform this
         * transition.
         */
        int updatedRows =
                shipmentRepository.markShippedAtomically(
                        shipmentId,
                        shippingLabel.carrier(),
                        shippingLabel.trackingNumber()
                );

        /*
         * We successfully performed the state transition.
         */
        if (updatedRows == 1) {

            Shipment shipment =
                    shipmentRepository.findById(shipmentId)
                            .orElseThrow(
                                    () ->
                                            new ShipmentNotFoundException(
                                                    shipmentId
                                            )
                            );

            /*
             * Persist the SHIPMENT_SHIPPED event in the SAME
             * transaction as the shipment state transition.
             *
             * Shipment UPDATE
             *        +
             * Outbox INSERT
             *        ↓
             *      COMMIT
             */
            shippingOutboxService
                    .createShipmentLifecycleEvent(
                            shipment,
                            ShippingEventType.SHIPMENT_SHIPPED.name()
                    );

            /*
             * IMPORTANT:
             *
             * This metric is recorded only when this request
             * actually performed PROCESSING -> SHIPPED.
             *
             * Therefore concurrent/idempotent retries cannot
             * inflate the metric.
             */
            shippingMetrics.shipmentShipped();

            return shipmentMapper.toResponse(
                    shipment
            );
        }

        /*
         * Another request already completed the transition.
         *
         * This is an idempotent success.
         *
         * IMPORTANT:
         *
         * Do NOT increment shipmentShipped().
         */
        Shipment currentShipment =
                shipmentRepository.findById(shipmentId)
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
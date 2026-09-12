package com.clothflow.shipping.mapper;

import com.clothflow.shipping.dto.response.ShipmentItemResponse;
import com.clothflow.shipping.dto.response.ShipmentResponse;
import com.clothflow.shipping.entity.Shipment;
import com.clothflow.shipping.entity.ShipmentItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShipmentMapper {

    public ShipmentResponse toResponse(
            Shipment shipment
    ) {

        List<ShipmentItemResponse> items =
                shipment.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getCustomerId(),
                shipment.getStatus(),
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getRecipientName(),
                shipment.getAddressLine1(),
                shipment.getAddressLine2(),
                shipment.getCity(),
                shipment.getState(),
                shipment.getPostalCode(),
                shipment.getCountry(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getVersion(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                items
        );
    }


    private ShipmentItemResponse toItemResponse(
            ShipmentItem item
    ) {

        return new ShipmentItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity()
        );
    }
}
package com.clothflow.shipping.provider;

import com.clothflow.shipping.entity.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CarrierTrackingWebhookRequest(

        @NotBlank(message = "Provider event ID is required")
        String providerEventId,

        @NotBlank(message = "Tracking number is required")
        String trackingNumber,

        @NotNull(message = "Status is required")
        ShipmentStatus status

) {}
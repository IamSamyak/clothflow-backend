package com.clothflow.shipping.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateShipmentRequest(

        @NotNull
        UUID orderId,

        @NotNull
        UUID customerId,

        @NotBlank
        @Size(max = 200)
        String recipientName,

        @NotBlank
        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 20)
        String postalCode,

        @NotBlank
        @Size(max = 100)
        String country,

        @NotEmpty
        @Valid
        List<ShipmentItemRequest> items
) {
}
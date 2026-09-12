package com.clothflow.shipping.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ShipmentItemRequest(

        @NotNull
        UUID productId,

        @NotBlank
        @Size(max = 255)
        String productName,

        @NotNull
        @Min(1)
        Integer quantity
) {
}
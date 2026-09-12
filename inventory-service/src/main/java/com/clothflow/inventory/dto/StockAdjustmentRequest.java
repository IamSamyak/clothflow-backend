package com.clothflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustmentRequest(

        @NotNull(message = "Operation ID is required")
        UUID operationId,

        @NotNull(message = "Quantity is required")
        @Min(
                value = 1,
                message = "Quantity must be greater than 0"
        )
        Long quantity
) {}
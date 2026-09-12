package com.clothflow.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustmentRequest(

        @NotNull
        UUID operationId,

        @Min(1)
        int quantity

) {
}
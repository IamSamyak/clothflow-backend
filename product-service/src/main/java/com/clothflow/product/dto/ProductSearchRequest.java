package com.clothflow.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductSearchRequest(

        @Size(
                max = 255,
                message = "Name must not exceed 255 characters"
        )
        String name,

        @DecimalMin(
                value = "0.00",
                message = "Minimum price cannot be negative"
        )
        BigDecimal minPrice,

        @DecimalMin(
                value = "0.00",
                message = "Maximum price cannot be negative"
        )
        BigDecimal maxPrice,

        Boolean inStock
) {

    public boolean hasInvalidPriceRange() {
        return minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0;
    }
}
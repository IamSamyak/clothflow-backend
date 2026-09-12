package com.clothflow.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "Shipping address is required")
        @Valid
        ShippingAddressRequest shippingAddress,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid CreateOrderItemRequest> items
) {
}
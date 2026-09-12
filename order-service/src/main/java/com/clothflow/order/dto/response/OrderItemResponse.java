package com.clothflow.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(

        UUID productId,

        String sku,

        String productName,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal subtotal
) {
}
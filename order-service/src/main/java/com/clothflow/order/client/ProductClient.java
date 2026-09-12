package com.clothflow.order.client;

import com.clothflow.order.dto.response.ProductResponse;

import java.util.UUID;

public interface ProductClient {

    ProductResponse getProduct(UUID productId);
}
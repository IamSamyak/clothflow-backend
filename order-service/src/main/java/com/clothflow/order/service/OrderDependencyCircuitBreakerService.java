package com.clothflow.order.service;

import com.clothflow.order.client.InventoryClient;
import com.clothflow.order.client.PaymentClient;
import com.clothflow.order.client.ProductClient;
import com.clothflow.order.dto.response.PaymentResponse;
import com.clothflow.order.exception.DownstreamServiceUnavailableException;
import com.clothflow.order.entity.Order;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderDependencyCircuitBreakerService {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderDependencyCircuitBreakerService(
            ProductClient productClient,
            InventoryClient inventoryClient,
            PaymentClient paymentClient
    ) {
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(
            name = "productService",
            fallbackMethod = "productServiceFallback"
    )
    public Object getProduct(
            UUID productId
    ) {

        return productClient.getProduct(
                productId
        );
    }

    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "inventoryServiceFallback"
    )
    public void reserveInventory(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        inventoryClient.reserve(
                productId,
                reservationId,
                quantity
        );
    }

    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "inventoryServiceFallbackVoid"
    )
    public void commitInventory(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        inventoryClient.commit(
                productId,
                reservationId,
                quantity
        );
    }

    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "inventoryServiceFallbackVoid"
    )
    public void releaseInventory(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        inventoryClient.release(
                productId,
                reservationId,
                quantity
        );
    }

    @CircuitBreaker(
            name = "paymentService",
            fallbackMethod = "paymentServiceFallback"
    )
    public PaymentResponse createPayment(
            Order order,
            String idempotencyKey
    ) {

        return paymentClient.createPayment(
                order,
                idempotencyKey
        );
    }

    private Object productServiceFallback(
            UUID productId,
            Throwable throwable
    ) {

        throw new DownstreamServiceUnavailableException(
                "Product Service"
        );
    }

    private void inventoryServiceFallback(
            UUID productId,
            UUID reservationId,
            int quantity,
            Throwable throwable
    ) {

        throw new DownstreamServiceUnavailableException(
                "Inventory Service"
        );
    }

    private void inventoryServiceFallbackVoid(
            UUID productId,
            UUID reservationId,
            int quantity,
            Throwable throwable
    ) {

        throw new DownstreamServiceUnavailableException(
                "Inventory Service"
        );
    }

    private PaymentResponse paymentServiceFallback(
            Order order,
            String idempotencyKey,
            Throwable throwable
    ) {

        throw new DownstreamServiceUnavailableException(
                "Payment Service"
        );
    }
}
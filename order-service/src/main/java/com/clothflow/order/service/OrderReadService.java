package com.clothflow.order.service;

import com.clothflow.order.dto.response.OrderItemResponse;
import com.clothflow.order.dto.response.OrderResponse;
import com.clothflow.order.entity.Order;
import com.clothflow.order.exception.OrderNotFoundException;
import com.clothflow.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderReadService {

    private final OrderRepository orderRepository;

    public OrderReadService(
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse getOrder(UUID orderId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        orderId
                                )
                        );

        return toResponse(order);
    }

    @Transactional
    public OrderResponse getOrder(
            UUID orderId,
            UUID customerId
    ) {

        Order order =
                orderRepository
                        .findByIdAndCustomerId(
                                orderId,
                                customerId
                        )
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        orderId
                                )
                        );

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items =
                order.getItems()
                        .stream()
                        .map(
                                item ->
                                        new OrderItemResponse(
                                                item.getProductId(),
                                                item.getSku(),
                                                item.getProductName(),
                                                item.getUnitPrice(),
                                                item.getQuantity(),
                                                item.getSubtotal()
                                        )
                        )
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

}
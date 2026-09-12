package com.clothflow.order.service;

import com.clothflow.order.client.ProductClient;
import com.clothflow.order.dto.request.CreateOrderItemRequest;
import com.clothflow.order.dto.request.CreateOrderRequest;
import com.clothflow.order.dto.request.ShippingAddressRequest;
import com.clothflow.order.dto.response.ProductResponse;
import com.clothflow.order.entity.*;
import com.clothflow.order.repository.OrderInventoryReservationRepository;
import com.clothflow.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderCreationPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderInventoryReservationRepository
            reservationRepository;
    private final ProductClient productClient;

    public OrderCreationPersistenceService(
            OrderRepository orderRepository,
            OrderInventoryReservationRepository reservationRepository,
            ProductClient productClient
    ) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.productClient = productClient;
    }

    @Transactional
    public Order createPendingOrder(
            CreateOrderRequest request,
            UUID customerId
    ) {

        String orderNumber =
                "CF-" +
                        UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

        ShippingAddressRequest address =
                request.shippingAddress();

        Order order = new Order(
                orderNumber,
                customerId,
                OrderStatus.PENDING,
                BigDecimal.ZERO,
                "INR",
                address.recipientName(),
                address.addressLine1(),
                address.addressLine2(),
                address.city(),
                address.state(),
                address.postalCode(),
                address.country()
        );

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest :
                request.items()) {

            ProductResponse product =
                    productClient.getProduct(
                            itemRequest.productId()
                    );

            BigDecimal subtotal =
                    product.price()
                            .multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.quantity()
                                    )
                            );

            OrderItem orderItem = new OrderItem(
                    product.id(),
                    product.sku(),
                    product.name(),
                    product.price(),
                    itemRequest.quantity(),
                    subtotal
            );

            order.addItem(orderItem);

            totalAmount =
                    totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        order.addStatusHistory(
                new OrderStatusHistory(
                        null,
                        OrderStatus.PENDING,
                        "Order created"
                )
        );

        Order savedOrder =
                orderRepository.saveAndFlush(order);

        for (OrderItem item :
                savedOrder.getItems()) {

            OrderInventoryReservation reservation =
                    new OrderInventoryReservation(
                            savedOrder,
                            item,
                            UUID.randomUUID(),
                            item.getProductId(),
                            item.getQuantity()
                    );

            reservationRepository.save(
                    reservation
            );
        }

        reservationRepository.flush();

        return savedOrder;
    }
}
package com.clothflow.order.entity;

public enum OrderStatus {

    PENDING,

    INVENTORY_RESERVED,

    PAYMENT_PENDING,

    CONFIRMED,

    PROCESSING,

    SHIPPED,

    DELIVERED,

    CANCELLED,

    INVENTORY_FAILED,

    PAYMENT_FAILED
}
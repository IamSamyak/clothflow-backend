package com.clothflow.order.entity;

public enum OutboxEventStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}
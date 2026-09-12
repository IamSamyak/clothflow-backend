package com.clothflow.shipping.entity;

public enum OutboxEventStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}
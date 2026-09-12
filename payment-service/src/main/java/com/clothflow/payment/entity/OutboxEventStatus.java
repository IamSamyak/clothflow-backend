package com.clothflow.payment.entity;

public enum OutboxEventStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}
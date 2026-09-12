package com.clothflow.inventory.entity;

public enum OutboxEventStatus {

    PENDING,

    PROCESSING,

    PUBLISHED,

    FAILED
}
package com.clothflow.user.service;

public enum RateLimitDecision {

    ALLOWED,

    IP_BLOCKED,

    EMAIL_BLOCKED,

    UNAVAILABLE
}
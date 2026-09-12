package com.clothflow.user.security;

public enum PasswordResetRateLimitDecision {

    ALLOWED,

    IP_BLOCKED,

    EMAIL_BLOCKED,

    UNAVAILABLE
}
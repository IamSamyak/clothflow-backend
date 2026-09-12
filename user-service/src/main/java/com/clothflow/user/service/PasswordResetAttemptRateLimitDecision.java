package com.clothflow.user.service;

public enum PasswordResetAttemptRateLimitDecision {

    ALLOWED,

    IP_BLOCKED,

    TOKEN_BLOCKED,

    UNAVAILABLE
}
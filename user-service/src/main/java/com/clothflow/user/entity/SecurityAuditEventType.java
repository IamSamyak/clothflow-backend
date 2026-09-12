package com.clothflow.user.entity;

public enum SecurityAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,

    REFRESH_SUCCESS,
    REFRESH_FAILURE,
    REFRESH_REUSE_DETECTED,

    LOGOUT,

    PASSWORD_CHANGED,
    PASSWORD_RESET,

    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    ACCOUNT_ACTIVATED,

    ROLE_ADDED,
    ROLE_REMOVED
}
package com.clothflow.user.exception;

public class RateLimitServiceException
        extends RuntimeException {

    public RateLimitServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
package com.clothflow.notification.provider;

public class NotificationProviderException
        extends RuntimeException {

    public NotificationProviderException(String message) {
        super(message);
    }

    public NotificationProviderException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
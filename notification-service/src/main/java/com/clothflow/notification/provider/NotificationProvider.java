package com.clothflow.notification.provider;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;

public interface NotificationProvider {

    NotificationChannel channel();

    void send(
            Notification notification,
            String idempotencyKey
    );
}
package com.clothflow.notification.provider;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FakeEmailNotificationProvider
        implements NotificationProvider {

    private static final Logger log =
            LoggerFactory.getLogger(
                    FakeEmailNotificationProvider.class
            );

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(
            Notification notification,
            String idempotencyKey
    ) {

        log.info(
                "Sending EMAIL notification: " +
                        "notificationId={}, " +
                        "customerId={}, " +
                        "subject={}, " +
                        "idempotencyKey={}",
                notification.getId(),
                notification.getCustomerId(),
                notification.getSubject(),
                idempotencyKey
        );

        log.info(
                "EMAIL content: {}",
                notification.getContent()
        );
    }
}
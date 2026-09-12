package com.clothflow.notification.service;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationDeliveryAttempt;
import com.clothflow.notification.provider.NotificationProvider;
import com.clothflow.notification.provider.NotificationProviderException;
import com.clothflow.notification.provider.NotificationProviderRegistry;
import org.springframework.stereotype.Service;

@Service
public class NotificationDeliveryService {

    private final NotificationProviderRegistry providerRegistry;
    private final NotificationDeliveryPersistenceService
            persistenceService;

    public NotificationDeliveryService(
            NotificationProviderRegistry providerRegistry,
            NotificationDeliveryPersistenceService persistenceService
    ) {
        this.providerRegistry =
                providerRegistry;

        this.persistenceService =
                persistenceService;
    }

    public void deliver(
            Notification notification
    ) {

        NotificationDeliveryAttempt attempt =
                persistenceService.startAttempt(
                        notification.getId()
                );

        String idempotencyKey =
                buildIdempotencyKey(notification);

        try {

            NotificationProvider provider =
                    providerRegistry.getProvider(
                            notification.getChannel()
                    );

            provider.send(
                    notification,
                    idempotencyKey
            );

            persistenceService.markSent(
                    notification.getId(),
                    attempt.getId()
            );

        } catch (NotificationProviderException ex) {

            persistenceService.markFailedOrRetry(
                    notification.getId(),
                    attempt.getId(),
                    ex.getMessage()
            );
        }
    }

    private String buildIdempotencyKey(
            Notification notification
    ) {

        return "clothflow:notification:" +
                notification.getId();
    }
}
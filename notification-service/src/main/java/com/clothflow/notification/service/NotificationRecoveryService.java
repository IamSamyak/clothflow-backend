package com.clothflow.notification.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationRecoveryService {

    private final NotificationDeliveryPersistenceService
            persistenceService;

    public NotificationRecoveryService(
            NotificationDeliveryPersistenceService persistenceService
    ) {
        this.persistenceService =
                persistenceService;
    }

    public int recoverStaleNotifications() {

        return persistenceService
                .recoverStaleNotifications();
    }
}
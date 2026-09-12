package com.clothflow.notification.service;

import com.clothflow.notification.config.NotificationProperties;
import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationClaimService {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;

    public NotificationClaimService(
            NotificationRepository notificationRepository,
            NotificationProperties properties
    ) {
        this.notificationRepository =
                notificationRepository;

        this.properties =
                properties;
    }

    @Transactional
    public List<Notification> claimBatch() {

        List<Notification> notifications =
                notificationRepository
                        .findEligibleNotificationsForUpdate(
                                properties.getDelivery()
                                        .getBatchSize()
                        );

        if (notifications.isEmpty()) {
            return notifications;
        }

        OffsetDateTime leaseUntil =
                OffsetDateTime.now()
                        .plusSeconds(
                                properties.getDelivery()
                                        .getLeaseSeconds()
                        );

        for (Notification notification :
                notifications) {

            notification.markProcessing(
                    leaseUntil
            );
        }

        notificationRepository.saveAll(
                notifications
        );

        return notifications;
    }
}
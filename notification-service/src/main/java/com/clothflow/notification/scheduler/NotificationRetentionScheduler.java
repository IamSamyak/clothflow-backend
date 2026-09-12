package com.clothflow.notification.scheduler;

import com.clothflow.notification.service.NotificationRetentionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetentionScheduler {

    private final NotificationRetentionService
            notificationRetentionService;

    public NotificationRetentionScheduler(
            NotificationRetentionService
                    notificationRetentionService
    ) {
        this.notificationRetentionService =
                notificationRetentionService;
    }

    @Scheduled(
            fixedDelayString =
                    "${notification.retention.fixed-delay:3600000}"
    )
    public void cleanupPasswordResetNotifications() {

        notificationRetentionService
                .deleteExpiredPasswordResetNotifications();
    }
}

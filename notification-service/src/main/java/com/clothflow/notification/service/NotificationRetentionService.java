package com.clothflow.notification.service;

import com.clothflow.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class NotificationRetentionService {

    private final NotificationRepository notificationRepository;

    private final long passwordResetRetentionDays;

    public NotificationRetentionService(
            NotificationRepository notificationRepository,
            @Value("${notification.retention.password-reset-days:1}")
            long passwordResetRetentionDays
    ) {
        this.notificationRepository =
                notificationRepository;

        this.passwordResetRetentionDays =
                passwordResetRetentionDays;
    }

    @Transactional
    public int deleteExpiredPasswordResetNotifications() {

        OffsetDateTime cutoff =
                OffsetDateTime.now()
                        .minusDays(
                                passwordResetRetentionDays
                        );

        return notificationRepository
                .deletePasswordResetNotificationsBefore(
                        cutoff
                );
    }
}
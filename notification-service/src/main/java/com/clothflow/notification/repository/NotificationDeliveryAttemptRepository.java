package com.clothflow.notification.repository;

import com.clothflow.notification.entity.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<
        NotificationDeliveryAttempt,
        UUID
        > {

    long countByNotificationId(UUID notificationId);
}
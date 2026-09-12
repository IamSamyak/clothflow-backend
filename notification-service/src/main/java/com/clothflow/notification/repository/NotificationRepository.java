package com.clothflow.notification.repository;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    List<Notification> findByStatusIn(
            Collection<NotificationStatus> statuses
    );

    @Query(
            value = """
                SELECT *
                FROM notifications
                WHERE status = 'PENDING'
                  AND next_attempt_at <= CURRENT_TIMESTAMP
                ORDER BY next_attempt_at ASC, created_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
                """,
            nativeQuery = true
    )
    List<Notification> findEligibleNotificationsForUpdate(
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.status = :processingStatus,
            n.nextAttemptAt = :leaseUntil,
            n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.id = :notificationId
          AND n.status = :pendingStatus
          AND n.nextAttemptAt <= CURRENT_TIMESTAMP
        """)
    int claimNotification(
            @Param("notificationId") UUID notificationId,
            @Param("pendingStatus") NotificationStatus pendingStatus,
            @Param("processingStatus") NotificationStatus processingStatus,
            @Param("leaseUntil") OffsetDateTime leaseUntil
    );

    @Query(
            value = """
                SELECT *
                FROM notifications
                WHERE status = 'PROCESSING'
                  AND next_attempt_at < CURRENT_TIMESTAMP
                ORDER BY next_attempt_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
                """,
            nativeQuery = true
    )
    List<Notification> findStaleProcessingNotificationsForUpdate(
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
        DELETE FROM Notification notification
        WHERE notification.eventType =
              'PASSWORD_RESET_REQUESTED'
          AND notification.createdAt < :cutoff
        """)
    int deletePasswordResetNotificationsBefore(
            @Param("cutoff") OffsetDateTime cutoff
    );
}
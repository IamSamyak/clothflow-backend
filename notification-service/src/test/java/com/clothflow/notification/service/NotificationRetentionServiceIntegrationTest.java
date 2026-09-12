package com.clothflow.notification.service;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;
import com.clothflow.notification.repository.NotificationDeliveryAttemptRepository;
import com.clothflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "notification.retention.password-reset-days=1"
        }
)
@ActiveProfiles("test")
class NotificationRetentionServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationRetentionService notificationRetentionService;

    @Autowired
    private NotificationDeliveryAttemptRepository
            notificationDeliveryAttemptRepository;

    @BeforeEach
    void setUp() {
        notificationDeliveryAttemptRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void shouldDeleteOldPasswordResetNotification() {

        Notification notification =
                createNotification(
                        UUID.randomUUID(),
                        "PASSWORD_RESET_REQUESTED"
                );

        notificationRepository.saveAndFlush(notification);

        setCreatedAt(
                notification,
                OffsetDateTime.now().minusDays(2)
        );

        notificationRepository.saveAndFlush(notification);

        int deleted =
                notificationRetentionService
                        .deleteExpiredPasswordResetNotifications();

        assertEquals(1, deleted);

        assertFalse(
                notificationRepository
                        .existsById(notification.getId())
        );
    }

    @Test
    void shouldKeepRecentPasswordResetNotification() {

        Notification notification =
                createNotification(
                        UUID.randomUUID(),
                        "PASSWORD_RESET_REQUESTED"
                );

        notificationRepository.saveAndFlush(notification);

        int deleted =
                notificationRetentionService
                        .deleteExpiredPasswordResetNotifications();

        assertEquals(0, deleted);

        assertTrue(
                notificationRepository
                        .existsById(notification.getId())
        );
    }

    @Test
    void shouldNotDeleteOldOrderConfirmedNotification() {

        Notification notification =
                createNotification(
                        UUID.randomUUID(),
                        "ORDER_CONFIRMED"
                );

        notificationRepository.saveAndFlush(notification);

        setCreatedAt(
                notification,
                OffsetDateTime.now().minusDays(2)
        );

        notificationRepository.saveAndFlush(notification);

        int deleted =
                notificationRetentionService
                        .deleteExpiredPasswordResetNotifications();

        assertEquals(0, deleted);

        assertTrue(
                notificationRepository
                        .existsById(notification.getId())
        );
    }

    @Test
    void shouldDeleteDeliveryAttemptsWithOldPasswordResetNotification() {

        Notification notification =
                createNotification(
                        UUID.randomUUID(),
                        "PASSWORD_RESET_REQUESTED"
                );

        notificationRepository.saveAndFlush(notification);

        setCreatedAt(
                notification,
                OffsetDateTime.now().minusDays(2)
        );

        notificationRepository.saveAndFlush(notification);

        int deleted =
                notificationRetentionService
                        .deleteExpiredPasswordResetNotifications();

        assertEquals(1, deleted);

        assertFalse(
                notificationRepository
                        .existsById(notification.getId())
        );

        assertEquals(
                0,
                notificationDeliveryAttemptRepository
                        .countByNotificationId(notification.getId())
        );
    }

    private Notification createNotification(
            UUID customerId,
            String eventType
    ) {
        return new Notification(
                customerId,
                NotificationChannel.EMAIL,
                eventType,
                "Test subject",
                "Test notification content"
        );
    }

    private void setCreatedAt(
            Notification notification,
            OffsetDateTime createdAt
    ) {
        try {
            Field field =
                    Notification.class.getDeclaredField("createdAt");

            field.setAccessible(true);
            field.set(notification, createdAt);

        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to modify createdAt for test",
                    exception
            );
        }
    }
}

package com.clothflow.notification.service;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;
import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.repository.NotificationRepository;
import com.clothflow.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "spring.task.scheduling.enabled=false"
        }
)
@Testcontainers
class NotificationEventServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("clothflow_notification_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.flyway.enabled",
                () -> true
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private NotificationEventService notificationEventService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {

        notificationRepository.deleteAll();

        processedEventRepository.deleteAll();
    }

    @Test
    void shouldPersistProcessedEventAndNotification() {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        boolean result =
                notificationEventService
                        .handlePasswordResetRequested(
                                eventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                userId,
                                event
                        );

        assertThat(result)
                .isTrue();

        assertThat(
                processedEventRepository
                        .findById(eventId)
        )
                .isPresent();

        assertThat(
                notificationRepository
                        .findAll()
        )
                .hasSize(1);

        Notification notification =
                notificationRepository
                        .findAll()
                        .getFirst();

        assertThat(notification.getCustomerId())
                .isEqualTo(userId);

        assertThat(notification.getChannel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(notification.getEventType())
                .isEqualTo(
                        "PASSWORD_RESET_REQUESTED"
                );
    }

    @Test
    void shouldIgnoreDuplicateEventUsingDatabaseIdempotency() {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        boolean firstResult =
                notificationEventService
                        .handlePasswordResetRequested(
                                eventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                userId,
                                event
                        );

        boolean secondResult =
                notificationEventService
                        .handlePasswordResetRequested(
                                eventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                userId,
                                event
                        );

        assertThat(firstResult)
                .isTrue();

        assertThat(secondResult)
                .isFalse();

        assertThat(
                processedEventRepository
                        .findAll()
        )
                .hasSize(1);

        assertThat(
                notificationRepository
                        .findAll()
        )
                .hasSize(1);
    }
}
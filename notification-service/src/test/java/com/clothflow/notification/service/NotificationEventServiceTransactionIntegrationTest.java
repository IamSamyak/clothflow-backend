package com.clothflow.notification.service;

import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.repository.NotificationRepository;
import com.clothflow.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "spring.task.scheduling.enabled=false"
        }
)
@Testcontainers
class NotificationEventServiceTransactionIntegrationTest {

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
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {

        notificationRepository.deleteAll();

        processedEventRepository.deleteAll();
    }

    @Test
    void shouldRollbackProcessedEventWhenNotificationPersistenceFails() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        assertThatThrownBy(() ->
                notificationEventService
                        .handlePasswordResetRequested(
                                eventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                userId,
                                event
                        )
        )
                .isInstanceOf(RuntimeException.class);

        assertThat(
                processedEventRepository
                        .findById(eventId)
        )
                .isEmpty();

        Integer notificationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM notifications
                        """,
                        Integer.class
                );

        assertThat(notificationCount)
                .isZero();
    }
}
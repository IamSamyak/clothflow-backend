package com.clothflow.notification.service;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.repository.NotificationRepository;
import com.clothflow.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.kafka.listener.auto-startup=false",
                "spring.task.scheduling.enabled=false"
        }
)
@Testcontainers
class NotificationEventServiceConcurrencyIntegrationTest {

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
    void shouldCreateOnlyOneNotificationForConcurrentDuplicateEvents()
            throws Exception {

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

        int threadCount = 2;

        CountDownLatch ready =
                new CountDownLatch(threadCount);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        try {

            List<Callable<Boolean>> tasks =
                    new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {

                tasks.add(() -> {

                    ready.countDown();

                    start.await();

                    return notificationEventService
                            .handlePasswordResetRequested(
                                    eventId,
                                    "PASSWORD_RESET_REQUESTED",
                                    "USER",
                                    userId,
                                    event
                            );
                });
            }

            List<Future<Boolean>> futures =
                    new ArrayList<>();

            for (Callable<Boolean> task : tasks) {
                futures.add(executor.submit(task));
            }

            ready.await();

            start.countDown();

            List<Boolean> results =
                    new ArrayList<>();

            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }

            long successful =
                    results.stream()
                            .filter(Boolean::booleanValue)
                            .count();

            long duplicates =
                    results.stream()
                            .filter(result -> !result)
                            .count();

            assertThat(successful)
                    .isEqualTo(1);

            assertThat(duplicates)
                    .isEqualTo(1);

            assertThat(
                    processedEventRepository.findAll()
            )
                    .hasSize(1);

            assertThat(
                    notificationRepository.findAll()
            )
                    .hasSize(1);

            Notification notification =
                    notificationRepository
                            .findAll()
                            .getFirst();

            assertThat(notification.getCustomerId())
                    .isEqualTo(userId);

            assertThat(notification.getEventType())
                    .isEqualTo(
                            "PASSWORD_RESET_REQUESTED"
                    );
        } finally {

            executor.shutdownNow();
        }
    }
}
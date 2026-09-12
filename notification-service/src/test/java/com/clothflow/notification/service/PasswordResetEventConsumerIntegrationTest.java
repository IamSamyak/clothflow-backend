package com.clothflow.notification.messaging;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.repository.NotificationRepository;
import com.clothflow.notification.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PasswordResetEventConsumerIntegrationTest {

    private static final String TOPIC =
            "clothflow.user.password-reset-requested";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("clothflow_notification_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "confluentinc/cp-kafka:7.6.1"
                    )
            );

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

        registry.add(
                "spring.kafka.bootstrap-servers",
                KAFKA::getBootstrapServers
        );

        registry.add(
                "spring.kafka.consumer.auto-offset-reset",
                () -> "earliest"
        );
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {

        notificationRepository.deleteAll();

        processedEventRepository.deleteAll();
    }

    @Test
    void shouldConsumePasswordResetEventAndCreateNotification()
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

        String payload =
                objectMapper.writeValueAsString(event);

        ProducerRecord<String, String> record =
                new ProducerRecord<>(
                        TOPIC,
                        userId.toString(),
                        payload
                );

        record.headers()
                .add(
                        new RecordHeader(
                                "event-id",
                                eventId.toString()
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        )
                );

        record.headers()
                .add(
                        new RecordHeader(
                                "event-type",
                                "PASSWORD_RESET_REQUESTED"
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        )
                );

        kafkaTemplate
                .send(record)
                .get();

        long deadline =
                System.currentTimeMillis()
                        + Duration.ofSeconds(15)
                        .toMillis();

        while (
                notificationRepository.count() == 0
                        && System.currentTimeMillis() < deadline
        ) {

            Thread.sleep(100);
        }

        assertThat(
                notificationRepository.count()
        )
                .isEqualTo(1);

        assertThat(
                processedEventRepository.count()
        )
                .isEqualTo(1);

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

        assertThat(notification.getContent())
                .contains(
                        "raw-reset-token-123"
                );
    }
}
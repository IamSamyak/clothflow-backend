package com.clothflow.notification.service;

import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationChannel;
import com.clothflow.notification.event.PasswordResetRequestedEvent;
import com.clothflow.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock
    private ProcessedEventService processedEventService;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationEventService notificationEventService;

    @BeforeEach
    void setUp() {

        notificationEventService =
                new NotificationEventService(
                        processedEventService,
                        notificationRepository
                );
    }

    @Test
    void shouldCreatePasswordResetNotificationForFirstEvent() {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        when(
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        userId
                )
        ).thenReturn(true);

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

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(
                        Notification.class
                );

        verify(notificationRepository)
                .save(captor.capture());

        Notification notification =
                captor.getValue();

        assertThat(notification.getCustomerId())
                .isEqualTo(userId);

        assertThat(notification.getChannel())
                .isEqualTo(NotificationChannel.EMAIL);

        assertThat(notification.getEventType())
                .isEqualTo(
                        "PASSWORD_RESET_REQUESTED"
                );

        assertThat(notification.getSubject())
                .isEqualTo(
                        "Reset your ClothFlow password"
                );

        assertThat(notification.getContent())
                .contains(
                        "raw-reset-token-123"
                );

        verify(
                processedEventService
        ).tryMarkProcessed(
                eventId,
                "PASSWORD_RESET_REQUESTED",
                "USER",
                userId
        );
    }

    @Test
    void shouldIgnoreDuplicatePasswordResetEvent() {

        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        when(
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        userId
                )
        ).thenReturn(false);

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
                .isFalse();

        verify(
                processedEventService
        ).tryMarkProcessed(
                eventId,
                "PASSWORD_RESET_REQUESTED",
                "USER",
                userId
        );

        verify(
                notificationRepository,
                never()
        ).save(any(Notification.class));
    }

    @Test
    void shouldRejectEventIdMismatch() {

        UUID headerEventId =
                UUID.randomUUID();

        UUID payloadEventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        payloadEventId,
                        userId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        when(
                processedEventService.tryMarkProcessed(
                        headerEventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        userId
                )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                notificationEventService
                        .handlePasswordResetRequested(
                                headerEventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                userId,
                                event
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Password reset eventId does not match payload eventId"
                );

        verify(
                notificationRepository,
                never()
        ).save(any(Notification.class));
    }

    @Test
    void shouldRejectAggregateIdMismatch() {

        UUID eventId =
                UUID.randomUUID();

        UUID aggregateId =
                UUID.randomUUID();

        UUID payloadUserId =
                UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        payloadUserId,
                        "customer@example.com",
                        "raw-reset-token-123"
                );

        when(
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        aggregateId
                )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                notificationEventService
                        .handlePasswordResetRequested(
                                eventId,
                                "PASSWORD_RESET_REQUESTED",
                                "USER",
                                aggregateId,
                                event
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Password reset aggregateId does not match userId"
                );

        verify(
                notificationRepository,
                never()
        ).save(any(Notification.class));
    }

    @Test
    void shouldRejectBlankResetToken() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "customer@example.com",
                        ""
                );

        when(
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        userId
                )
        ).thenReturn(true);

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
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Password reset token must not be blank"
                );

        verify(
                notificationRepository,
                never()
        ).save(any(Notification.class));
    }

    @Test
    void shouldRejectBlankEmail() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(
                        eventId,
                        userId,
                        "",
                        "raw-reset-token-123"
                );

        when(
                processedEventService.tryMarkProcessed(
                        eventId,
                        "PASSWORD_RESET_REQUESTED",
                        "USER",
                        userId
                )
        ).thenReturn(true);

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
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Password reset email must not be blank"
                );

        verify(
                notificationRepository,
                never()
        ).save(any(Notification.class));
    }
}
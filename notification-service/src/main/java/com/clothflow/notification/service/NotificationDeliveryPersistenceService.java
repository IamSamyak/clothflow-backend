package com.clothflow.notification.service;

import com.clothflow.notification.config.NotificationProperties;
import com.clothflow.notification.entity.DeliveryAttemptStatus;
import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.entity.NotificationDeliveryAttempt;
import com.clothflow.notification.entity.NotificationStatus;
import com.clothflow.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDeliveryPersistenceService {

    private final NotificationRepository notificationRepository;

    private final NotificationProperties properties;

    public NotificationDeliveryPersistenceService(
            NotificationRepository notificationRepository,
            NotificationProperties properties
    ) {
        this.notificationRepository =
                notificationRepository;

        this.properties =
                properties;
    }

    @Transactional
    public NotificationDeliveryAttempt startAttempt(
            UUID notificationId
    ) {

        Notification notification =
                notificationRepository.findById(
                        notificationId
                ).orElseThrow();

        if (notification.getStatus()
                != NotificationStatus.PROCESSING) {

            throw new IllegalStateException(
                    "Notification is not in PROCESSING state: "
                            + notificationId
            );
        }

        int attemptNumber =
                notification.getDeliveryAttempts()
                        .size() + 1;

        NotificationDeliveryAttempt attempt =
                new NotificationDeliveryAttempt(
                        attemptNumber,
                        DeliveryAttemptStatus.STARTED,
                        notification.getLastError()
                );

        notification.addDeliveryAttempt(
                attempt
        );

        notificationRepository.save(
                notification
        );

        return attempt;
    }

    @Transactional
    public void markSent(
            UUID notificationId,
            UUID attemptId
    ) {

        Notification notification =
                notificationRepository.findById(
                        notificationId
                ).orElseThrow();

        if (notification.getStatus()
                != NotificationStatus.PROCESSING) {

            return;
        }

        NotificationDeliveryAttempt attempt =
                notification.getDeliveryAttempts()
                        .stream()
                        .filter(existingAttempt ->
                                existingAttempt.getId()
                                        .equals(attemptId)
                        )
                        .findFirst()
                        .orElseThrow();

        attempt.markSent();

        notification.markSent();

        notificationRepository.save(
                notification
        );
    }

    @Transactional
    public int recoverStaleNotifications() {

        List<Notification> staleNotifications =
                notificationRepository
                        .findStaleProcessingNotificationsForUpdate(
                                properties.getDelivery()
                                        .getBatchSize()
                        );

        if (staleNotifications.isEmpty()) {
            return 0;
        }

        for (Notification notification :
                staleNotifications) {

            notification.getDeliveryAttempts()
                    .stream()
                    .filter(attempt ->
                            attempt.getStatus()
                                    == DeliveryAttemptStatus.STARTED
                    )
                    .reduce(
                            (first, second) -> second
                    )
                    .ifPresent(attempt ->
                            attempt.markAbandoned(
                                    "Delivery worker lease expired"
                            )
                    );

            notification.markForRetry(
                    OffsetDateTime.now(),
                    "Delivery worker lease expired"
            );
        }

        notificationRepository.saveAll(
                staleNotifications
        );

        return staleNotifications.size();
    }

    @Transactional
    public void markFailedOrRetry(
            UUID notificationId,
            UUID attemptId,
            String error
    ) {

        Notification notification =
                notificationRepository.findById(
                        notificationId
                ).orElseThrow();

        if (notification.getStatus()
                != NotificationStatus.PROCESSING) {

            return;
        }

        NotificationDeliveryAttempt attempt =
                notification.getDeliveryAttempts()
                        .stream()
                        .filter(existingAttempt ->
                                existingAttempt.getId()
                                        .equals(attemptId)
                        )
                        .findFirst()
                        .orElseThrow();

        attempt.markFailed(error);

        int retryCount =
                notification.getRetryCount() + 1;

        if (retryCount >= properties.getDelivery().getMaxRetries()) {

            notification.markFailed(error);

        } else {

            OffsetDateTime nextAttemptAt =
                    OffsetDateTime.now()
                            .plusSeconds(
                                    calculateBackoffSeconds(
                                            retryCount
                                    )
                            );

            notification.markForRetry(
                    nextAttemptAt,
                    error
            );
        }

        notificationRepository.save(
                notification
        );
    }

    private long calculateBackoffSeconds(
            int retryCount
    ) {

        NotificationProperties.RetryBackoff backoff =
                properties.getDelivery()
                        .getRetryBackoff();

        return switch (retryCount) {

            case 1 ->
                    backoff.getFirstRetrySeconds();

            case 2 ->
                    backoff.getSecondRetrySeconds();

            case 3 ->
                    backoff.getThirdRetrySeconds();

            case 4 ->
                    backoff.getFourthRetrySeconds();

            default ->
                    backoff.getSubsequentRetrySeconds();
        };
    }
}
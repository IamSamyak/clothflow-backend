package com.clothflow.notification.scheduler;

import com.clothflow.notification.service.NotificationRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecoveryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationRecoveryScheduler.class
            );

    private final NotificationRecoveryService recoveryService;

    public NotificationRecoveryScheduler(
            NotificationRecoveryService recoveryService
    ) {
        this.recoveryService =
                recoveryService;
    }

    @Scheduled(
            fixedDelayString =
                    "${notification.recovery.scheduler-delay-ms}"
    )
    public void recoverStaleNotifications() {

        int recovered =
                recoveryService.recoverStaleNotifications();

        if (recovered > 0) {

            log.info(
                    "Recovered {} stale PROCESSING notifications",
                    recovered
            );
        }
    }
}
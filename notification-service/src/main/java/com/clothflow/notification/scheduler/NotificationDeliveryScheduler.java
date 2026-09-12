package com.clothflow.notification.scheduler;

import com.clothflow.notification.config.NotificationProperties;
import com.clothflow.notification.entity.Notification;
import com.clothflow.notification.service.NotificationClaimService;
import com.clothflow.notification.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationDeliveryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    NotificationDeliveryScheduler.class
            );

    private final NotificationClaimService claimService;
    private final NotificationDeliveryService deliveryService;
    private final ThreadPoolTaskExecutor notificationTaskExecutor;
    private final NotificationProperties properties;

    public NotificationDeliveryScheduler(
            NotificationClaimService claimService,
            NotificationDeliveryService deliveryService,
            ThreadPoolTaskExecutor notificationTaskExecutor,
            NotificationProperties properties
    ) {
        this.claimService = claimService;
        this.deliveryService = deliveryService;
        this.notificationTaskExecutor =
                notificationTaskExecutor;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString =
                    "${notification.delivery.scheduler-delay-ms}"
    )
    public void processNotifications() {

        List<Notification> notifications =
                claimService.claimBatch();

        if (notifications.isEmpty()) {
            return;
        }

        log.info(
                "Claimed {} notifications for delivery",
                notifications.size()
        );

        for (Notification notification :
                notifications) {

            notificationTaskExecutor.execute(
                    () -> deliverNotification(notification)
            );
        }
    }

    private void deliverNotification(
            Notification notification
    ) {

        try {

            deliveryService.deliver(
                    notification
            );

        } catch (Exception ex) {

            log.error(
                    "Unexpected notification delivery error: " +
                            "notificationId={}",
                    notification.getId(),
                    ex
            );
        }
    }
}
package com.clothflow.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class NotificationExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor notificationTaskExecutor(
            NotificationProperties properties
    ) {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        int workerThreads =
                properties.getDelivery()
                        .getWorkerThreads();

        executor.setCorePoolSize(workerThreads);
        executor.setMaxPoolSize(workerThreads);

        /*
         * Do not allow an unbounded queue.
         *
         * The database batch size and worker pool
         * together provide backpressure.
         */
        executor.setQueueCapacity(
                properties.getDelivery()
                        .getBatchSize()
        );

        executor.setThreadNamePrefix(
                "notification-worker-"
        );

        executor.setWaitForTasksToCompleteOnShutdown(
                true
        );

        executor.setAwaitTerminationSeconds(
                30
        );

        executor.initialize();

        return executor;
    }
}
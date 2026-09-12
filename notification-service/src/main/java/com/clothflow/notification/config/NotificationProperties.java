package com.clothflow.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "notification")
@Validated
public class NotificationProperties {

    @Valid
    private Delivery delivery =
            new Delivery();

    @Valid
    private Recovery recovery =
            new Recovery();

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(
            Delivery delivery
    ) {
        this.delivery =
                delivery;
    }

    public Recovery getRecovery() {
        return recovery;
    }

    public void setRecovery(
            Recovery recovery
    ) {
        this.recovery =
                recovery;
    }

    public static class Delivery {

        @Min(1)
        private int batchSize;

        @Min(1)
        private long leaseSeconds;

        @Min(1)
        private int maxRetries;

        @Min(100)
        private long schedulerDelayMs;

        @Min(1)
        private int workerThreads;

        @Valid
        private RetryBackoff retryBackoff =
                new RetryBackoff();

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(
                int batchSize
        ) {
            this.batchSize = batchSize;
        }

        public long getLeaseSeconds() {
            return leaseSeconds;
        }

        public void setLeaseSeconds(
                long leaseSeconds
        ) {
            this.leaseSeconds = leaseSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(
                int maxRetries
        ) {
            this.maxRetries = maxRetries;
        }

        public long getSchedulerDelayMs() {
            return schedulerDelayMs;
        }

        public void setSchedulerDelayMs(
                long schedulerDelayMs
        ) {
            this.schedulerDelayMs = schedulerDelayMs;
        }

        public int getWorkerThreads() {
            return workerThreads;
        }

        public void setWorkerThreads(
                int workerThreads
        ) {
            this.workerThreads = workerThreads;
        }

        public RetryBackoff getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(
                RetryBackoff retryBackoff
        ) {
            this.retryBackoff = retryBackoff;
        }
    }

    public static class Recovery {

        @Min(100)
        private long schedulerDelayMs;

        public long getSchedulerDelayMs() {
            return schedulerDelayMs;
        }

        public void setSchedulerDelayMs(
                long schedulerDelayMs
        ) {
            this.schedulerDelayMs =
                    schedulerDelayMs;
        }
    }

    public static class RetryBackoff {

        @Min(0)
        private long firstRetrySeconds;

        @Min(0)
        private long secondRetrySeconds;

        @Min(0)
        private long thirdRetrySeconds;

        @Min(0)
        private long fourthRetrySeconds;

        @Min(0)
        private long subsequentRetrySeconds;

        public long getFirstRetrySeconds() {
            return firstRetrySeconds;
        }

        public void setFirstRetrySeconds(
                long firstRetrySeconds
        ) {
            this.firstRetrySeconds =
                    firstRetrySeconds;
        }

        public long getSecondRetrySeconds() {
            return secondRetrySeconds;
        }

        public void setSecondRetrySeconds(
                long secondRetrySeconds
        ) {
            this.secondRetrySeconds =
                    secondRetrySeconds;
        }

        public long getThirdRetrySeconds() {
            return thirdRetrySeconds;
        }

        public void setThirdRetrySeconds(
                long thirdRetrySeconds
        ) {
            this.thirdRetrySeconds =
                    thirdRetrySeconds;
        }

        public long getFourthRetrySeconds() {
            return fourthRetrySeconds;
        }

        public void setFourthRetrySeconds(
                long fourthRetrySeconds
        ) {
            this.fourthRetrySeconds =
                    fourthRetrySeconds;
        }

        public long getSubsequentRetrySeconds() {
            return subsequentRetrySeconds;
        }

        public void setSubsequentRetrySeconds(
                long subsequentRetrySeconds
        ) {
            this.subsequentRetrySeconds =
                    subsequentRetrySeconds;
        }
    }
}
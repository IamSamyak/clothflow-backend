package com.clothflow.payment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter paymentsCreated;
    private final Counter paymentsSucceeded;
    private final Counter paymentsFailed;

    private final Timer paymentProcessingTimer;

    public PaymentMetrics(MeterRegistry meterRegistry) {

        this.paymentsCreated =
                Counter.builder("clothflow.payment.created")
                        .description("Number of payment records created")
                        .register(meterRegistry);

        this.paymentsSucceeded =
                Counter.builder("clothflow.payment.succeeded")
                        .description("Number of payments successfully completed")
                        .register(meterRegistry);

        this.paymentsFailed =
                Counter.builder("clothflow.payment.failed")
                        .description("Number of payments that failed")
                        .register(meterRegistry);

        this.paymentProcessingTimer =
                Timer.builder("clothflow.payment.processing")
                        .description(
                                "Time taken to process a payment through the payment gateway"
                        )
                        .publishPercentileHistogram()
                        .register(meterRegistry);
    }

    public void paymentCreated() {
        paymentsCreated.increment();
    }

    public void paymentSucceeded() {
        paymentsSucceeded.increment();
    }

    public void paymentFailed() {
        paymentsFailed.increment();
    }

    public Timer.Sample startProcessingTimer() {
        return Timer.start();
    }

    public void recordProcessing(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }
}
package com.clothflow.order.client;

import com.clothflow.order.dto.request.CreatePaymentRequest;
import com.clothflow.order.dto.response.PaymentResponse;
import com.clothflow.order.entity.Order;
import com.clothflow.order.entity.PaymentMethod;
import com.clothflow.order.exception.PaymentServiceException;
import com.clothflow.order.security.ServiceTokenProvider;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentServiceClient
        implements PaymentClient {

    private final RestClient restClient;

    private final String paymentServiceBaseUrl;

    private final ServiceTokenProvider serviceTokenProvider;

    public PaymentServiceClient(
            RestClient paymentRestClient,
            ServiceTokenProvider serviceTokenProvider,
            @Value("${payment.service.base-url}")
            String paymentServiceBaseUrl
    ) {
        this.restClient = paymentRestClient;
        this.serviceTokenProvider = serviceTokenProvider;
        this.paymentServiceBaseUrl =
                paymentServiceBaseUrl;
    }

    @Override
    @Bulkhead(
            name = "paymentService"
    )
    @CircuitBreaker(
            name = "paymentService",
            fallbackMethod = "createPaymentFallback"
    )
    public PaymentResponse createPayment(
            Order order,
            String idempotencyKey
    ) {

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        order.getId(),
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        PaymentMethod.UPI
                );

        try {

            return restClient
                    .post()
                    .uri(
                            paymentServiceBaseUrl
                                    + "/api/v1/payments"
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                                    + serviceTokenProvider
                                    .getAccessToken()
                    )
                    .header(
                            "Idempotency-Key",
                            idempotencyKey
                    )
                    .body(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (requestMessage, response) -> {

                                throw new PaymentServiceException(
                                        "Payment Service rejected payment request with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (requestMessage, response) -> {

                                throw new PaymentServiceException(
                                        "Payment Service is currently unavailable"
                                );
                            }
                    )
                    .body(PaymentResponse.class);

        } catch (PaymentServiceException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new PaymentServiceException(
                    "Failed to communicate with Payment Service",
                    ex
            );
        }
    }

    private PaymentResponse createPaymentFallback(
            Order order,
            String idempotencyKey,
            Throwable throwable
    ) {

        throw new PaymentServiceException(
                "Payment Service is temporarily unavailable",
                throwable
        );
    }
}
package com.clothflow.order.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CorrelationIdInterceptor
        implements ClientHttpRequestInterceptor {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-Id";

    private static final String MDC_KEY =
            "correlationId";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        String correlationId = MDC.get(MDC_KEY);

        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(
                    CORRELATION_ID_HEADER,
                    correlationId
            );
        }

        return execution.execute(request, body);
    }
}
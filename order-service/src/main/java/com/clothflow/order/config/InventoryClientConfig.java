package com.clothflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class InventoryClientConfig {

    @Bean
    public RestClient inventoryRestClient(
            RestClient.Builder builder,
            CorrelationIdInterceptor correlationIdInterceptor
    ) {
        return builder
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }
}
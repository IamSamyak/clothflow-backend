package com.clothflow.order.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientTimeoutConfig {

    private static final int CONNECT_TIMEOUT_SECONDS = 2;
    private static final int RESPONSE_TIMEOUT_SECONDS = 5;

    @Bean
    public CloseableHttpClient clothFlowHttpClient() {

        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setConnectionRequestTimeout(
                                Timeout.ofSeconds(
                                        CONNECT_TIMEOUT_SECONDS
                                )
                        )
                        .setResponseTimeout(
                                Timeout.ofSeconds(
                                        RESPONSE_TIMEOUT_SECONDS
                                )
                        )
                        .build();

        return HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder
                                .create()
                                .setMaxConnTotal(100)
                                .setMaxConnPerRoute(20)
                                .build()
                )
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder(
            CloseableHttpClient clothFlowHttpClient
    ) {

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(
                        clothFlowHttpClient
                );

        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
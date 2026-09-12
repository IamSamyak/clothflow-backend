package com.clothflow.order.client;

import com.clothflow.order.dto.response.ProductResponse;
import com.clothflow.order.exception.ProductNotFoundException;
import com.clothflow.order.exception.ProductServiceException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ProductServiceClient
        implements ProductClient {

    private final RestClient restClient;

    private final String productServiceBaseUrl;

    public ProductServiceClient(
            RestClient productRestClient,
            @Value("${product.service.base-url}")
            String productServiceBaseUrl
    ) {

        this.restClient = productRestClient;
        this.productServiceBaseUrl =
                productServiceBaseUrl;
    }

    @Override
    @Retry(
            name = "productService"
    )
    @Bulkhead(
            name = "productService"
    )
    @CircuitBreaker(
            name = "productService",
            fallbackMethod = "getProductFallback"
    )
    public ProductResponse getProduct(
            UUID productId
    ) {

        try {

            return restClient
                    .get()
                    .uri(
                            productServiceBaseUrl
                                    + "/api/v1/products/{productId}",
                            productId
                    )
                    .retrieve()

                    .onStatus(
                            status -> status.value() == 404,
                            (request, response) -> {

                                throw new ProductNotFoundException(
                                        productId
                                );
                            }
                    )

                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {

                                throw new ProductServiceException(
                                        "Product Service rejected the request with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )

                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {

                                throw new ProductServiceException(
                                        "Product Service is currently unavailable"
                                );
                            }
                    )

                    .body(ProductResponse.class);

        } catch (
                ProductNotFoundException |
                ProductServiceException ex
        ) {

            throw ex;

        } catch (Exception ex) {

            throw new ProductServiceException(
                    "Failed to communicate with Product Service",
                    ex
            );
        }
    }

    private ProductResponse getProductFallback(
            UUID productId,
            Throwable throwable
    ) {

        throw new ProductServiceException(
                "Product Service is temporarily unavailable",
                throwable
        );
    }
}
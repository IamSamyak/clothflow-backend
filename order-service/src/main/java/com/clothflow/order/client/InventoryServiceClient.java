package com.clothflow.order.client;

import com.clothflow.order.dto.request.StockAdjustmentRequest;
import com.clothflow.order.exception.InventoryInsufficientStockException;
import com.clothflow.order.exception.InventoryServiceException;
import com.clothflow.order.security.ServiceTokenProvider;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class InventoryServiceClient
        implements InventoryClient {

    private final RestClient restClient;

    private final String inventoryServiceBaseUrl;

    private final ServiceTokenProvider serviceTokenProvider;

    public InventoryServiceClient(
            RestClient inventoryRestClient,
            ServiceTokenProvider serviceTokenProvider,
            @Value("${inventory.service.base-url}")
            String inventoryServiceBaseUrl
    ) {
        this.restClient = inventoryRestClient;
        this.serviceTokenProvider = serviceTokenProvider;
        this.inventoryServiceBaseUrl =
                inventoryServiceBaseUrl;
    }

    @Override
    @Bulkhead(
            name = "inventoryService"
    )
    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "reserveFallback"
    )
    public void reserve(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            inventoryServiceBaseUrl
                                    + "/api/v1/inventory/{productId}/reservations",
                            productId
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                                    + serviceTokenProvider
                                    .getAccessToken()
                    )
                    .body(
                            new ReservationRequest(
                                    reservationId,
                                    quantity
                            )
                    )
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 409,
                            (request, response) -> {
                                throw new InventoryInsufficientStockException(
                                        productId
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service rejected reservation request with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service is currently unavailable"
                                );
                            }
                    )
                    .toBodilessEntity();

        } catch (
                InventoryInsufficientStockException |
                InventoryServiceException ex
        ) {

            throw ex;

        } catch (Exception ex) {

            throw new InventoryServiceException(
                    "Failed to communicate with Inventory Service",
                    ex
            );
        }
    }

    @Override
    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "releaseFallback"
    )
    public void release(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            inventoryServiceBaseUrl
                                    + "/api/v1/inventory/{productId}/reservations/release",
                            productId
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                                    + serviceTokenProvider
                                    .getAccessToken()
                    )
                    .body(
                            new ReservationRequest(
                                    reservationId,
                                    quantity
                            )
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service rejected reservation release with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service is currently unavailable during reservation release"
                                );
                            }
                    )
                    .toBodilessEntity();

        } catch (InventoryServiceException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new InventoryServiceException(
                    "Failed to communicate with Inventory Service during reservation release",
                    ex
            );
        }
    }

    @Override
    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "commitFallback"
    )
    public void commit(
            UUID productId,
            UUID reservationId,
            int quantity
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            inventoryServiceBaseUrl
                                    + "/api/v1/inventory/{productId}/reservations/commit",
                            productId
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                                    + serviceTokenProvider
                                    .getAccessToken()
                    )
                    .body(
                            new ReservationRequest(
                                    reservationId,
                                    quantity
                            )
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service rejected reservation commit with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service is currently unavailable during reservation commit"
                                );
                            }
                    )
                    .toBodilessEntity();

        } catch (InventoryServiceException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new InventoryServiceException(
                    "Failed to communicate with Inventory Service during reservation commit",
                    ex
            );
        }
    }

    @Override
    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "addStockFallback"
    )
    public void addStock(
            UUID productId,
            UUID operationId,
            int quantity
    ) {

        try {

            restClient
                    .post()
                    .uri(
                            inventoryServiceBaseUrl
                                    + "/api/v1/inventory/{productId}/stock/add",
                            productId
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                                    + serviceTokenProvider
                                    .getAccessToken()
                    )
                    .body(
                            new StockAdjustmentRequest(
                                    operationId,
                                    quantity
                            )
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service rejected stock restoration with status "
                                                + response.getStatusCode().value()
                                );
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new InventoryServiceException(
                                        "Inventory Service is currently unavailable during stock restoration"
                                );
                            }
                    )
                    .toBodilessEntity();

        } catch (InventoryServiceException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new InventoryServiceException(
                    "Failed to communicate with Inventory Service during stock restoration",
                    ex
            );
        }
    }

    private void reserveFallback(
            UUID productId,
            UUID reservationId,
            int quantity,
            Throwable throwable
    ) {

        throw new InventoryServiceException(
                "Inventory Service is temporarily unavailable during reservation",
                throwable
        );
    }

    private void releaseFallback(
            UUID productId,
            UUID reservationId,
            int quantity,
            Throwable throwable
    ) {

        throw new InventoryServiceException(
                "Inventory Service is temporarily unavailable during reservation release",
                throwable
        );
    }

    private void commitFallback(
            UUID productId,
            UUID reservationId,
            int quantity,
            Throwable throwable
    ) {

        throw new InventoryServiceException(
                "Inventory Service is temporarily unavailable during reservation commit",
                throwable
        );
    }

    private void addStockFallback(
            UUID productId,
            UUID operationId,
            int quantity,
            Throwable throwable
    ) {

        throw new InventoryServiceException(
                "Inventory Service is temporarily unavailable during stock restoration",
                throwable
        );
    }

    private record ReservationRequest(
            UUID reservationId,
            int quantity
    ) {
    }
}
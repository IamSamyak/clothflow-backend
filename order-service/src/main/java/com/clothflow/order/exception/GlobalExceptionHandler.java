package com.clothflow.order.exception;

import com.clothflow.order.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException exception
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>>
    handleInvalidOrderStatusTransition(
            InvalidOrderStatusTransitionException exception
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_ORDER_STATUS_TRANSITION",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> validationErrors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "VALIDATION_FAILED");
        response.put("message", "Request validation failed");
        response.put("fieldErrors", validationErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String error,
            String message
    ) {

        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", OffsetDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(
            ProductNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        404,
                        "PRODUCT_NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleProductServiceException(
            ProductServiceException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        503,
                        "PRODUCT_SERVICE_UNAVAILABLE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InventoryInsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse>
    handleInventoryInsufficientStock(
            InventoryInsufficientStockException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ApiErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "INVENTORY_INSUFFICIENT",
                                ex.getMessage(),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<ApiErrorResponse>
    handleInventoryServiceException(
            InventoryServiceException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        new ApiErrorResponse(
                                OffsetDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "INVENTORY_SERVICE_UNAVAILABLE",
                                ex.getMessage(),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(
            PaymentServiceException.class
    )
    public ResponseEntity<Map<String, Object>>
    handlePaymentServiceException(
            PaymentServiceException ex
    ) {

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "PAYMENT_SERVICE_UNAVAILABLE",
                ex.getMessage()
        );
    }
}
package com.clothflow.payment.exception;

import com.clothflow.payment.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            PaymentNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handlePaymentNotFound(
            PaymentNotFoundException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IdempotencyConflictException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIdempotencyConflict(
            IdempotencyConflictException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(
            IllegalStateException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_PAYMENT_STATE",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse>
    buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {

        ApiErrorResponse response =
                new ApiErrorResponse(
                        OffsetDateTime.now(),
                        status.value(),
                        error,
                        message,
                        path
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(
            RefundAlreadyExistsException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRefundAlreadyExists(
            RefundAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "REFUND_ALREADY_EXISTS",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}
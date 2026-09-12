package com.clothflow.shipping.exception;

import com.clothflow.shipping.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(
            ShipmentNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleShipmentNotFound(
            ShipmentNotFoundException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "SHIPMENT_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(
            ShipmentForOrderNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleShipmentForOrderNotFound(
            ShipmentForOrderNotFoundException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "SHIPMENT_FOR_ORDER_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(
            ShipmentAlreadyExistsException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleShipmentAlreadyExists(
            ShipmentAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "SHIPMENT_ALREADY_EXISTS",
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(
            InvalidShipmentStatusTransitionException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleInvalidTransition(
            InvalidShipmentStatusTransitionException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "INVALID_SHIPMENT_STATUS_TRANSITION",
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse(
                                "Request validation failed"
                        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );
    }


    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
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
}
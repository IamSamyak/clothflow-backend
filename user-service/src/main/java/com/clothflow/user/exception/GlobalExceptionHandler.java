package com.clothflow.user.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationFailed(
            AuthenticationFailedException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.UNAUTHORIZED
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/unauthorized"
                )
        );

        problem.setTitle(
                "Unauthorized"
        );

        problem.setDetail(
                ex.getMessage()
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(problem);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRequests(
            TooManyRequestsException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.TOO_MANY_REQUESTS
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/too-many-requests"
                )
        );

        problem.setTitle(
                "Too Many Requests"
        );

        problem.setDetail(
                ex.getMessage()
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        "60"
                )
                .body(problem);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/conflict"
                )
        );

        problem.setTitle(
                "Conflict"
        );

        problem.setDetail(
                ex.getMessage()
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/not-found"
                )
        );

        problem.setTitle(
                "Resource Not Found"
        );

        problem.setDetail(
                ex.getMessage()
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/bad-request"
                )
        );

        problem.setTitle(
                "Bad Request"
        );

        problem.setDetail(
                ex.getMessage()
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        String detail =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .collect(
                                Collectors.joining(", ")
                        );

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );

        problem.setType(
                URI.create(
                        "https://clothflow.dev/problems/validation"
                )
        );

        problem.setTitle(
                "Validation Failed"
        );

        problem.setDetail(
                detail
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }
}
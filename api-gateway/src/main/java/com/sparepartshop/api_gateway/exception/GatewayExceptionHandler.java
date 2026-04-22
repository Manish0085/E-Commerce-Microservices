package com.sparepartshop.api_gateway.exception;

import com.sparepartshop.api_gateway.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(
            ResourceAccessException ex,
            HttpServletRequest request) {

        String serviceName = extractServiceName(request.getRequestURI());
        log.error("Downstream service unavailable: {} | Path: {}", serviceName, request.getRequestURI());

        String message;
        if (ex.getCause() instanceof ConnectException) {
            message = String.format(
                    "Service '%s' is currently unavailable. Please try again later.",
                    serviceName
            );
        } else {
            message = String.format(
                    "Unable to reach service '%s': %s",
                    serviceName,
                    ex.getMessage()
            );
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .service(serviceName)
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(
            RestClientException ex,
            HttpServletRequest request) {

        String serviceName = extractServiceName(request.getRequestURI());
        log.error("Error communicating with service '{}': {}", serviceName, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error(HttpStatus.BAD_GATEWAY.getReasonPhrase())
                .message("Error communicating with downstream service: " + serviceName)
                .path(request.getRequestURI())
                .service(serviceName)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        Throwable cause = ex.getCause();
        if (cause instanceof ResourceAccessException rae) {
            return handleResourceAccessException(rae, request);
        }
        if (cause instanceof RestClientException rce) {
            return handleRestClientException(rce, request);
        }

        log.error("Unexpected error at gateway for path: {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred at the gateway.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at gateway for path: {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred at the gateway.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String extractServiceName(String path) {
        if (path == null || !path.startsWith("/api/v1/")) {
            return "unknown";
        }
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return "unknown";
        }
        return switch (parts[3]) {
            case "products" -> "product-service";
            case "inventory" -> "inventory-service";
            case "customers" -> "customer-service";
            case "orders" -> "order-service";
            case "invoices" -> "billing-service";
            case "payments" -> "payment-service";
            default -> parts[3];
        };
    }
}

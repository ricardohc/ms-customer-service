package com.banking.customer.exception;

import com.banking.customer.dto.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String NOT_FOUND_CODE = "CUSTOMER_NOT_FOUND";
    private static final String CONFLICT_CODE = "CUSTOMER_CONFLICT";
    private static final String INTERNAL_CODE = "INTERNAL_ERROR";

    @ExceptionHandler(CustomerNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(CustomerNotFoundException ex,
                                                              ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, NOT_FOUND_CODE, ex.getMessage(), exchange);
    }

    @ExceptionHandler(CustomerConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConflict(CustomerConflictException ex,
                                                              ServerWebExchange exchange) {
        return build(HttpStatus.CONFLICT, CONFLICT_CODE, ex.getMessage(), exchange);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                ServerWebExchange exchange) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (ex.getMostSpecificCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            Object[] values = ife.getTargetType().getEnumConstants();
            message = "Unsupported value '" + ife.getValue() + "' for field '" + ife.getPathReference()
                    + "'. Supported values: " + Arrays.toString(values);
        }
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, exchange);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex,
                                                                     ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_CODE,
                "Unexpected internal error", exchange);
    }

    private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String code, String message,
                                                      ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        ErrorResponse error = new ErrorResponse();
        error.setCode(code);
        error.setMessage(message);
        error.setTimestamp(OffsetDateTime.now());
        error.setCorrelationId(correlationId);
        error.setDetails(List.of());
        return Mono.just(ResponseEntity.status(status).body(error));
    }
}

package com.lab.message.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex, ServerHttpRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    ResponseEntity<ApiError> unavailable(DownstreamUnavailableException ex, ServerHttpRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiError> invalid(WebExchangeBindException ex, ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации запроса", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, ServerHttpRequest request) {
        return ResponseEntity.status(status).body(new ApiError(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getPath().value()));
    }
}

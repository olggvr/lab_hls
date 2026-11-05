package com.lab.message.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ApiError> notFound(NotFoundException ex, ServerHttpRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Mono<ApiError> unavailable(DownstreamUnavailableException ex, ServerHttpRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> invalid(WebExchangeBindException ex, ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации запроса", request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> invalidParameter(HandlerMethodValidationException ex, ServerHttpRequest request) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации параметров: " + message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> invalidDomain(ConstraintViolationException ex, ServerHttpRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации доменной модели: " + ex.getMessage(), request);
    }

    private Mono<ApiError> build(HttpStatus status, String message, ServerHttpRequest request) {
        return Mono.just(new ApiError(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, request.getPath().value()));
    }
}

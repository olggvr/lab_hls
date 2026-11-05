package com.lab.assignment.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Mono<ApiError> notFound(NotFoundException ex, ServerWebExchange exchange) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Mono<ApiError> conflict(ConflictException ex, ServerWebExchange exchange) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(DownstreamUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Mono<ApiError> unavailable(DownstreamUnavailableException ex, ServerWebExchange exchange) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), exchange, Map.of());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> validation(WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Ошибка валидации", exchange, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> methodValidation(HandlerMethodValidationException ex, ServerWebExchange exchange) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Ошибка валидации параметров: " + message, exchange, Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Mono<ApiError> domainValidation(ConstraintViolationException ex, ServerWebExchange exchange) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Ошибка валидации доменной модели", exchange, fields);
    }

    private Mono<ApiError> error(HttpStatus status, String message, ServerWebExchange exchange,
                                 Map<String, String> fields) {
        return Mono.just(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, exchange.getRequest().getPath().value(), fields));
    }
}

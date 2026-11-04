package com.lab.grades.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
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

    private Mono<ApiError> error(HttpStatus status, String message, ServerWebExchange exchange,
                                 Map<String, String> fields) {
        return Mono.just(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, exchange.getRequest().getPath().value(), fields));
    }
}

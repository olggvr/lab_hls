package com.lab.school.exception;

import com.lab.school.dto.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    /**
     * Ошибки валидации @Valid @RequestBody.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации запроса", request, errors);
    }

    /**
     * Ошибки валидации параметров метода контроллера (@RequestParam, @PathVariable с @Min/@Max и т.п.).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
                                                           HttpServletRequest request) {
        String message = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации параметров: " + message, request, null);
    }

    /**
     * Ошибки валидации Entity (Bean Validation на уровне JPA перед сохранением).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации данных", request, toFieldErrors(ex));
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiError> handleTransactionSystem(TransactionSystemException ex,
                                                            HttpServletRequest request) {
        Throwable cause = ex.getRootCause();
        if (cause instanceof ConstraintViolationException cve) {
            return handleConstraintViolation(cve, request);
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Операция нарушает целостность данных (дубликат или есть связанные записи)", request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Некорректное тело запроса (JSON)", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Некорректное значение параметра '" + ex.getName() + "'", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Отсутствует обязательный параметр '" + ex.getParameterName() + "'", request, null);
    }

    /**
     * Остальные исключения. Стандартные исключения Spring MVC (404 на неизвестный путь, 405, 415 и т.д.)
     * реализуют {@link ErrorResponse} и сохраняют свой HTTP-статус.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return build(HttpStatus.valueOf(status.value()), detail != null ? detail : ex.getMessage(), request, null);
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, null);
    }

    private static Map<String, String> toFieldErrors(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }
        return errors;
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message,
                                                  HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}

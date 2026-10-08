package com.lab.school.exception;

/**
 * Нарушение бизнес-правила (запрос корректен синтаксически, но не может быть выполнен) -> 422.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

package com.lab.school.exception;

/**
 * Конфликт с текущим состоянием данных (дубликат и т.п.) -> 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

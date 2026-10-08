package com.lab.school.exception;

/**
 * Запрошенная сущность не найдена -> 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " с id=" + id + " не найден(а)");
    }

    public NotFoundException(String message) {
        super(message);
    }
}

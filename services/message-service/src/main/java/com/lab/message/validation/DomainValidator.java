package com.lab.message.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DomainValidator {
    private final Validator validator;
    private final boolean enabled;

    public DomainValidator(
            Validator validator,
            @Value("${app.validation.domain.enabled:true}") boolean enabled) {
        this.validator = validator;
        this.enabled = enabled;
    }

    public <T> void validate(T value) {
        if (!enabled) {
            return;
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}

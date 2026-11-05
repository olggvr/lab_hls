package com.lab.school.integration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentReferenceRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Positive Long classId) {
}

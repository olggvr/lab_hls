package com.lab.school.integration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentReferenceRequest(
        @NotNull @Positive Long teacherId,
        @NotNull @Positive Long classId,
        @NotNull @Positive Long subjectId) {
}

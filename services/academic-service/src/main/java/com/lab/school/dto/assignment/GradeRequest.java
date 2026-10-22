package com.lab.school.dto.assignment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GradeRequest(
        @NotNull @Positive Long studentId,
        @NotNull @Min(1) @Max(5) Integer grade
) {
}

package com.lab.school.dto.student;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentCreateRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long classId
) {
}

package com.lab.school.dto.teacher;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeacherCreateRequest(
        @NotNull @Positive Long userId,
        @Positive Long subjectId
) {
}

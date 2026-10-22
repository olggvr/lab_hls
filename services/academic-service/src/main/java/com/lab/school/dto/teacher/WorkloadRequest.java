package com.lab.school.dto.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record WorkloadRequest(
        @NotNull @Positive Long classId,
        @NotNull @Positive Long subjectId,
        @NotBlank @Pattern(regexp = "\\d{4}/\\d{4}", message = "формат учебного года: 2026/2027")
        String academicYear
) {
}

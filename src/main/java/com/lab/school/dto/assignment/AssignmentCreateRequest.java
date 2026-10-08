package com.lab.school.dto.assignment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignmentCreateRequest(
        @NotNull @Positive Long teacherId,
        @NotNull @Positive Long classId,
        @NotNull @Positive Long subjectId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull @FutureOrPresent LocalDate dueDate
) {
}

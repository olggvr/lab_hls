package com.lab.school.dto.student;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StudentUpdateRequest(@NotNull @Positive Long classId) {
}

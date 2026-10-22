package com.lab.school.dto.teacher;

import jakarta.validation.constraints.Positive;

public record TeacherUpdateRequest(@Positive Long subjectId) {
}

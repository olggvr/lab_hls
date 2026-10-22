package com.lab.school.dto.assignment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssignmentResponse(
        Long id,
        Long teacherId,
        String teacherName,
        Long classId,
        String className,
        Long subjectId,
        String subjectName,
        String title,
        String description,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
}

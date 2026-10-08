package com.lab.school.dto.student;

public record StudentResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        Long classId,
        String className
) {
}

package com.lab.school.dto.teacher;

public record TeacherResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone,
        Long subjectId,
        String subjectName
) {
}

package com.lab.school.dto.teacher;

public record WorkloadResponse(
        Long id,
        Long teacherId,
        Long classId,
        String className,
        Long subjectId,
        String subjectName,
        String academicYear
) {
}

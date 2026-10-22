package com.lab.school.integration;

public record AssignmentReferenceResponse(
        Long teacherId,
        Long teacherUserId,
        String teacherName,
        Long classId,
        String className,
        Long subjectId,
        String subjectName) {
}

package com.lab.assignment.api;

import java.time.LocalDateTime;

public record GradingContextResponse(
        Long submissionId,
        Long assignmentId,
        Long studentId,
        String studentName,
        String contentUrl,
        LocalDateTime submittedAt,
        Long teacherId,
        Long teacherUserId,
        String teacherName,
        String assignmentTitle,
        String subjectName) {
}

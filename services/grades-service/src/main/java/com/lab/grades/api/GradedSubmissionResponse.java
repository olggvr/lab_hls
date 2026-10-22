package com.lab.grades.api;

import java.time.LocalDateTime;

public record GradedSubmissionResponse(
        Long id, Long assignmentId, Long studentId, String studentName, String contentUrl,
        LocalDateTime submittedAt, Integer grade, String status, LocalDateTime gradedAt) {
}

package com.lab.assignment.api;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id, Long assignmentId, Long studentId, String studentName, String contentUrl,
        LocalDateTime submittedAt, Integer grade, String status, LocalDateTime gradedAt) {
}

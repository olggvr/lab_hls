package com.lab.school.dto.assignment;

import com.lab.school.entity.enums.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        Long studentId,
        String studentName,
        String contentUrl,
        LocalDateTime submittedAt,
        Integer grade,
        SubmissionStatus status,
        LocalDateTime gradedAt
) {
}

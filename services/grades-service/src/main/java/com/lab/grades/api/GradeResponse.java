package com.lab.grades.api;

public record GradeResponse(
        GradedSubmissionResponse submission,
        Long notificationId,
        int notifiedRecipients) {
}

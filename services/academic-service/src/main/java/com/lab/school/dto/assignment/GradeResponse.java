package com.lab.school.dto.assignment;

/**
 * Результат выставления оценки: обновлённое решение + созданное уведомление.
 */
public record GradeResponse(
        SubmissionResponse submission,
        Long notificationId,
        int notifiedRecipients
) {
}

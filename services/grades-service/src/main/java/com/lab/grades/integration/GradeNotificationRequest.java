package com.lab.grades.integration;

import java.util.List;

public record GradeNotificationRequest(
        Long senderId, String senderName, String subject, String body, List<Long> recipientIds) {
}

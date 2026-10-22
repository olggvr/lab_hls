package com.lab.grades.integration;

public record NotificationResponse(Long messageId, int recipientsCount, boolean delivered) {
}

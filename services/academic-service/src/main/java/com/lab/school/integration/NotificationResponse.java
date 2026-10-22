package com.lab.school.integration;

public record NotificationResponse(Long messageId, int recipientsCount, boolean delivered) {
}

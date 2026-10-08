package com.lab.school.dto.message;

import com.lab.school.entity.enums.MessageType;

import java.time.LocalDateTime;

public record InboxItemResponse(
        Long messageId,
        Long senderId,
        String senderName,
        String subject,
        String body,
        MessageType type,
        LocalDateTime createdAt,
        boolean read,
        LocalDateTime readAt
) {
}

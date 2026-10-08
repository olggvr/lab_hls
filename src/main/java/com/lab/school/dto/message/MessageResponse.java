package com.lab.school.dto.message;

import com.lab.school.entity.enums.MessageType;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String subject,
        String body,
        MessageType type,
        LocalDateTime createdAt,
        int recipientsCount
) {
}

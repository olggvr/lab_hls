package com.lab.message.api;

import com.lab.message.domain.MessageType;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id, Long senderId, String senderName, String subject, String body,
        MessageType type, LocalDateTime createdAt, int recipientsCount
) { }

package com.lab.message.api;

import com.lab.message.domain.MessageType;

import java.time.LocalDateTime;

public record InboxItemResponse(
        Long messageId, Long senderId, String senderName, String subject, String body,
        MessageType type, LocalDateTime createdAt, boolean read, LocalDateTime readAt
) { }

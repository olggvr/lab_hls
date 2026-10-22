package com.lab.message.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("messages")
public record MessageEntity(
        @Id Long id,
        @Column("sender_id") Long senderId,
        @Column("sender_name") String senderName,
        String subject,
        String body,
        MessageType type,
        @Column("created_at") LocalDateTime createdAt
) {
    public MessageEntity(Long senderId, String senderName, String subject, String body, MessageType type) {
        this(null, senderId, senderName, subject, body, type, LocalDateTime.now());
    }
}

package com.lab.message.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("messages")
public record MessageEntity(
        @Id Long id,
        @Column("sender_id") @NotNull @Positive Long senderId,
        @Column("sender_name") @NotBlank @Size(max = 150) String senderName,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String body,
        @NotNull MessageType type,
        @Column("created_at") @NotNull @PastOrPresent LocalDateTime createdAt
) {
    public MessageEntity(Long senderId, String senderName, String subject, String body, MessageType type) {
        this(null, senderId, senderName, subject, body, type, LocalDateTime.now());
    }
}

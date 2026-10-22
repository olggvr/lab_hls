package com.lab.message.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("message_recipients")
public record RecipientEntity(
        @Id Long id,
        @Column("message_id") Long messageId,
        @Column("user_id") Long userId,
        @Column("is_read") boolean read,
        @Column("read_at") LocalDateTime readAt
) {
    public RecipientEntity(Long messageId, Long userId) {
        this(null, messageId, userId, false, null);
    }

    public RecipientEntity markRead() {
        return read ? this : new RecipientEntity(id, messageId, userId, true, LocalDateTime.now());
    }
}

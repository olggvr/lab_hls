package com.lab.message.domain;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("message_recipients")
public record RecipientEntity(
        @Id Long id,
        @Column("message_id") @NotNull @Positive Long messageId,
        @Column("user_id") @NotNull @Positive Long userId,
        @Column("is_read") boolean read,
        @Column("read_at") @PastOrPresent LocalDateTime readAt
) {
    public RecipientEntity(Long messageId, Long userId) {
        this(null, messageId, userId, false, null);
    }

    public RecipientEntity markRead() {
        return read ? this : new RecipientEntity(id, messageId, userId, true, LocalDateTime.now());
    }

    @AssertTrue(message = "readAt должен быть задан только для прочитанного сообщения")
    public boolean isReadStateValid() {
        return read == (readAt != null);
    }
}

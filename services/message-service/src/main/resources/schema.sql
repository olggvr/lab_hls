CREATE TABLE IF NOT EXISTS messages
(
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT       NOT NULL,
    sender_name VARCHAR(150) NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,
    type        VARCHAR(30)  NOT NULL CHECK (type IN ('DIRECT', 'BROADCAST', 'GRADE_NOTIFICATION')),
    created_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages (sender_id);

CREATE TABLE IF NOT EXISTS message_recipients
(
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT    NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    BIGINT    NOT NULL,
    is_read    BOOLEAN   NOT NULL DEFAULT FALSE,
    read_at    TIMESTAMP,
    CONSTRAINT uq_message_recipient UNIQUE (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_message_recipients_user ON message_recipients (user_id);

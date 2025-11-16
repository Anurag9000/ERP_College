CREATE TABLE IF NOT EXISTS instructor_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instructor_username VARCHAR(64) NOT NULL,
    section_id VARCHAR(32) NOT NULL,
    subject VARCHAR(128) NOT NULL,
    body TEXT NOT NULL,
    recipient_ids TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_instructor_messages_instructor (instructor_username)
);

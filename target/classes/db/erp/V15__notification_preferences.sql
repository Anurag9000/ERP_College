CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id VARCHAR(64) PRIMARY KEY,
    digest_frequency VARCHAR(16) NOT NULL DEFAULT 'IMMEDIATE',
    digest_hour INT NOT NULL DEFAULT 8,
    email_enabled BIT NOT NULL DEFAULT 0,
    sms_enabled BIT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    -- Note: Foreign key to users(username) removed because it crosses database boundaries
    -- Application layer must ensure referential integrity
);

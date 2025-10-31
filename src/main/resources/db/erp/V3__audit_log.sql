CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(32) NOT NULL,
    actor VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_events_created (created_at),
    INDEX idx_audit_events_type (event_type)
);

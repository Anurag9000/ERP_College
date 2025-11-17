CREATE TABLE IF NOT EXISTS maintenance_windows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    message VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    created_by VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maintenance_window_status ON maintenance_windows(status, start_at);

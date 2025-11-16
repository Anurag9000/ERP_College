CREATE TABLE IF NOT EXISTS fee_schedule_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    offset_days INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_template_course FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE
);

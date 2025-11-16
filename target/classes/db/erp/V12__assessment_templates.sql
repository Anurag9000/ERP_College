CREATE TABLE IF NOT EXISTS assessment_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    template_name VARCHAR(64) NOT NULL,
    weights_json TEXT NOT NULL,
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_template_course_name (course_code, template_name)
);

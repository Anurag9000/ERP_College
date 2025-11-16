ALTER TABLE sections ADD COLUMN requires_advisor_approval TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS registration_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    section_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_by VARCHAR(64),
    decided_by VARCHAR(64),
    decided_at TIMESTAMP NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_student FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE,
    CONSTRAINT fk_req_section FOREIGN KEY (section_code) REFERENCES sections(section_code) ON DELETE CASCADE,
    UNIQUE KEY uq_registration_request (student_code, section_code)
);

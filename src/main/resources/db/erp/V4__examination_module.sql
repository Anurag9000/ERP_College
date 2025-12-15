CREATE TABLE IF NOT EXISTS exam_forms (
    form_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    semester VARCHAR(16) NOT NULL,
    year INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SUBMITTED', -- SUBMITTED, APPROVED, REJECTED
    exam_fee_paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_registrations (
    registration_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    form_id BIGINT NOT NULL,
    section_code VARCHAR(32) NOT NULL,
    FOREIGN KEY (form_id) REFERENCES exam_forms(form_id) ON DELETE CASCADE,
    FOREIGN KEY (section_code) REFERENCES sections(section_code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS admit_cards (
    admit_card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    form_id BIGINT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pdf_path VARCHAR(512),
    FOREIGN KEY (form_id) REFERENCES exam_forms(form_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_schedule (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(32) NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(64),
    FOREIGN KEY (section_code) REFERENCES sections(section_code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mark_sheets (
    mark_sheet_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    semester VARCHAR(16) NOT NULL,
    year INT NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pdf_path VARCHAR(512),
    sgpa DECIMAL(4,2),
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE
);

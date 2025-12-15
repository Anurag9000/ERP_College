CREATE TABLE IF NOT EXISTS assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATETIME NOT NULL,
    max_marks DECIMAL(5,2) NOT NULL,
    assignment_type VARCHAR(32) NOT NULL, -- ASSIGNMENT, TEST, QUIZ, PROJECT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (section_code) REFERENCES sections(section_code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    submission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_code VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_path VARCHAR(512),
    marks_obtained DECIMAL(5,2),
    feedback TEXT,
    status VARCHAR(20) DEFAULT 'SUBMITTED', -- SUBMITTED, GRADED, LATE
    FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE,
    UNIQUE KEY uq_submission (assignment_id, student_code)
);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(32) NOT NULL, -- DEPARTMENT, UNION, COLLEGE, UNIVERSITY, SOCIETY
    department VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    posted_by VARCHAR(64) NOT NULL,
    posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'NORMAL', -- HIGH, NORMAL, LOW
    INDEX idx_category (category),
    INDEX idx_department (department)
);

CREATE TABLE IF NOT EXISTS announcement_subscriptions (
    subscription_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    department VARCHAR(64),
    subscribed BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE,
    UNIQUE KEY uq_subscription (student_code, category, department)
);

-- Seed some sample assignments
INSERT INTO assignments (section_code, title, description, due_date, max_marks, assignment_type)
SELECT section_code, 'Assignment 1', 'First assignment', DATE_ADD(NOW(), INTERVAL 7 DAY), 100, 'ASSIGNMENT'
FROM sections
LIMIT 3;

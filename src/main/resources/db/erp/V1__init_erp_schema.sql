CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    phone VARCHAR(32),
    date_of_birth DATE,
    address VARCHAR(255),
    course_code VARCHAR(32),
    semester INT,
    status VARCHAR(32),
    fees_paid DECIMAL(12,2) DEFAULT 0,
    total_fees DECIMAL(12,2) DEFAULT 0,
    cgpa DECIMAL(4,2) DEFAULT 0,
    credits_completed INT DEFAULT 0,
    credits_in_progress INT DEFAULT 0,
    next_fee_due DATE,
    advisor_id VARCHAR(32),
    academic_standing VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS instructors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instructor_code VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    phone VARCHAR(32),
    department VARCHAR(64),
    designation VARCHAR(64),
    qualification VARCHAR(64),
    status VARCHAR(32),
    joining_date DATE,
    salary DECIMAL(12,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL UNIQUE,
    course_name VARCHAR(128) NOT NULL,
    department VARCHAR(64),
    duration_semesters INT,
    fees DECIMAL(12,2),
    description TEXT,
    total_seats INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(32) NOT NULL UNIQUE,
    course_code VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    instructor_code VARCHAR(32),
    day_of_week VARCHAR(16),
    start_time TIME,
    end_time TIME,
    location VARCHAR(64),
    capacity INT,
    enrollment_deadline DATE,
    drop_deadline DATE,
    semester VARCHAR(16),
    year INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    section_code VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    final_grade DECIMAL(5,2),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    component VARCHAR(64) NOT NULL,
    score DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_grade_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(32) NOT NULL,
    attendance_date DATE NOT NULL,
    student_code VARCHAR(32) NOT NULL,
    present BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audience VARCHAR(16) NOT NULL,
    target_id VARCHAR(64),
    message TEXT NOT NULL,
    category VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(128) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS instructor_office_hours (
    office_hour_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instructor_code VARCHAR(32) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(100),
    FOREIGN KEY (instructor_code) REFERENCES instructors(instructor_code) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_code VARCHAR(32) NOT NULL,
    instructor_code VARCHAR(32) NOT NULL,
    office_hour_id BIGINT,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    purpose VARCHAR(255),
    rejection_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_code) REFERENCES students(student_code) ON DELETE CASCADE,
    FOREIGN KEY (instructor_code) REFERENCES instructors(instructor_code) ON DELETE CASCADE,
    FOREIGN KEY (office_hour_id) REFERENCES instructor_office_hours(office_hour_id) ON DELETE SET NULL
);

-- Seed some sample office hours
INSERT INTO instructor_office_hours (instructor_code, day_of_week, start_time, end_time, location)
SELECT instructor_code, 'MONDAY', '14:00:00', '16:00:00', 'Room 305'
FROM instructors
ORDER BY instructor_code
LIMIT 1;

INSERT INTO instructor_office_hours (instructor_code, day_of_week, start_time, end_time, location)
SELECT instructor_code, 'WEDNESDAY', '10:00:00', '12:00:00', 'Room 305'
FROM instructors
ORDER BY instructor_code
LIMIT 1;

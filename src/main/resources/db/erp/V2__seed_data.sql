INSERT INTO courses (course_code, course_name, department, duration_semesters, fees, description, total_seats)
VALUES
    ('CSE101', 'Computer Science Engineering', 'Computer Science', 8, 200000, '4-year undergraduate program in Computer Science', 60),
    ('MATH101', 'Mathematics', 'Mathematics', 6, 150000, '3-year undergraduate program in Mathematics', 40)
ON DUPLICATE KEY UPDATE
    course_name = VALUES(course_name),
    department = VALUES(department),
    duration_semesters = VALUES(duration_semesters),
    fees = VALUES(fees),
    description = VALUES(description),
    total_seats = VALUES(total_seats);

INSERT INTO students (student_code, auth_username, first_name, last_name, email, phone, course_code, semester, status, fees_paid, total_fees, cgpa, credits_completed, credits_in_progress, next_fee_due, academic_standing)
VALUES
    ('STU001', 'stu1', 'Alice', 'Johnson', 'alice.johnson@student.college.edu', '987-654-3210', 'CSE101', 3, 'Active', 150000, 200000, 7.8, 72, 18, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 'Good'),
    ('STU002', 'stu2', 'Bob', 'Williams', 'bob.williams@student.college.edu', '987-654-3211', 'MATH101', 2, 'Active', 100000, 150000, 8.4, 36, 18, DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'Good')
ON DUPLICATE KEY UPDATE
    auth_username = VALUES(auth_username),
    course_code = VALUES(course_code),
    semester = VALUES(semester),
    status = VALUES(status),
    fees_paid = VALUES(fees_paid),
    total_fees = VALUES(total_fees),
    cgpa = VALUES(cgpa),
    credits_completed = VALUES(credits_completed),
    credits_in_progress = VALUES(credits_in_progress),
    next_fee_due = VALUES(next_fee_due),
    academic_standing = VALUES(academic_standing);

INSERT INTO instructors (instructor_code, auth_username, first_name, last_name, email, phone, department, designation, qualification, status, joining_date, salary)
VALUES
    ('FAC001', 'inst1', 'John', 'Smith', 'john.smith@college.edu', '123-456-7890', 'Computer Science', 'Professor', 'Ph.D', 'Active', CURDATE(), 75000)
ON DUPLICATE KEY UPDATE
    auth_username = VALUES(auth_username),
    department = VALUES(department),
    designation = VALUES(designation),
    status = VALUES(status),
    qualification = VALUES(qualification),
    salary = VALUES(salary);

INSERT INTO sections (section_code, course_code, title, instructor_code, day_of_week, start_time, end_time, location, capacity, enrollment_deadline, drop_deadline, semester, year)
VALUES
    ('SEC101A', 'CSE101', 'Data Structures - A', 'FAC001', 'MONDAY', '09:00', '10:30', 'Room CS-101', 30, DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'Fall', YEAR(CURDATE())),
    ('SEC101B', 'CSE101', 'Algorithms - A', 'FAC001', 'WEDNESDAY', '11:00', '12:30', 'Room CS-201', 30, DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'Fall', YEAR(CURDATE())),
    ('SEC201A', 'MATH101', 'Statistics - A', NULL, 'TUESDAY', '14:00', '15:30', 'Room MATH-101', 25, DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'Fall', YEAR(CURDATE()))
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    instructor_code = VALUES(instructor_code),
    day_of_week = VALUES(day_of_week),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    location = VALUES(location),
    capacity = VALUES(capacity),
    semester = VALUES(semester),
    year = VALUES(year);

INSERT INTO enrollments (student_code, section_code, status, final_grade)
VALUES
    ('STU001', 'SEC101A', 'ENROLLED', NULL),
    ('STU002', 'SEC201A', 'ENROLLED', NULL),
    ('STU002', 'SEC101B', 'WAITLISTED', NULL)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    final_grade = VALUES(final_grade);

INSERT INTO section_waitlist (section_code, student_code, position)
SELECT 'SEC101B', 'STU002', 1
WHERE NOT EXISTS (SELECT 1 FROM section_waitlist WHERE section_code = 'SEC101B' AND student_code = 'STU002');

INSERT INTO notifications (audience, target_id, message, category)
VALUES
    ('ALL', NULL, '📅 Semester opens next Monday. Check your timetable for clashes.', 'General'),
    ('STUDENT', 'STU001', CONCAT('Fees due soon. Outstanding balance ₹', (SELECT total_fees - fees_paid FROM students WHERE student_code = 'STU001')), 'Finance'),
    ('STUDENT', 'STU002', 'You are waitlisted for Algorithms - A. We will auto-enrol if a seat frees up.', 'Registration');

INSERT INTO settings (setting_key, setting_value)
VALUES
    ('maintenance', 'false')
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value);

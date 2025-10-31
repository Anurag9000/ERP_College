INSERT INTO users (username, password_hash, salt, role, full_name, email, active, must_change_password)
VALUES
    ('admin', 'mvwtteEDcGlHg3B1Uyvg7Rw/TMbJyZCw41px3XmYG40=', 'wnNEdMkMN3GoHWu5ZvW1vg==', 'Admin', 'Administrator', 'admin@college.edu', TRUE, FALSE),
    ('inst1', '0Jb2tXj2vCvDPRTGMYCCMioczhEw6uUaFSmTBbw9ODo=', 'fKLBKnPZnSCbJiilkGcYzA==', 'Instructor', 'John Smith', 'john.smith@college.edu', TRUE, FALSE),
    ('stu1', 'tMYgivNxruroEL57/TGYcWBQ51e2ERqQ28reZqf+DWc=', '+GXg1huCTnr22koG3OCKTQ==', 'Student', 'Alice Johnson', 'alice.johnson@student.college.edu', TRUE, FALSE),
    ('stu2', 'AornhHo1QvIaTknXJ3Oxz3I81ojDu1prGV0Q0/aI6tM=', 'cUTyNK0NP0tMSDIJVr27Aw==', 'Student', 'Bob Williams', 'bob.williams@student.college.edu', TRUE, FALSE)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    role = VALUES(role),
    active = VALUES(active);

INSERT INTO password_history (user_id, password_hash, salt)
SELECT u.id, u.password_hash, u.salt
FROM users u
WHERE u.username IN ('admin', 'inst1', 'stu1', 'stu2')
  AND NOT EXISTS (
        SELECT 1 FROM password_history ph
        WHERE ph.user_id = u.id AND ph.password_hash = u.password_hash
  );

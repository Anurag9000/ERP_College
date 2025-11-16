CREATE TABLE IF NOT EXISTS course_corequisites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    corequisite_code VARCHAR(32) NOT NULL,
    CONSTRAINT fk_coreq_course FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    CONSTRAINT fk_coreq_required FOREIGN KEY (corequisite_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    UNIQUE KEY uq_coreq (course_code, corequisite_code)
);

CREATE TABLE IF NOT EXISTS course_antirequisites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    antirequisite_code VARCHAR(32) NOT NULL,
    CONSTRAINT fk_antireq_course FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    CONSTRAINT fk_antireq_block FOREIGN KEY (antirequisite_code) REFERENCES courses(course_code) ON DELETE CASCADE,
    UNIQUE KEY uq_antireq (course_code, antirequisite_code)
);

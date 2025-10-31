ALTER TABLE courses
    ADD COLUMN credit_hours INT NOT NULL DEFAULT 3;

UPDATE courses
SET credit_hours = 3
WHERE credit_hours IS NULL;

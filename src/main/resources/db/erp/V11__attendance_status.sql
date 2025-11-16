ALTER TABLE attendance_records
    ADD COLUMN status VARCHAR(16);

UPDATE attendance_records
SET status = CASE WHEN present THEN 'PRESENT' ELSE 'ABSENT' END
WHERE status IS NULL;

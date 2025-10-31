-- Helper script to reset baseline seed data for local development.
-- Flyway treats repeatable migrations as idempotent; this script is for manual execution only.
-- Execute via `flyway -locations=classpath:db/erp -target=R__truncate_seed_tables` if supported,
-- or run the statements below against the ERP schema directly.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE attendance_records;
TRUNCATE TABLE section_waitlist;
TRUNCATE TABLE grades;
TRUNCATE TABLE enrollments;
TRUNCATE TABLE section_assessments;
TRUNCATE TABLE maintenance_schedule;
TRUNCATE TABLE notifications;
TRUNCATE TABLE fee_payments;
TRUNCATE TABLE settings;
TRUNCATE TABLE sections;
TRUNCATE TABLE courses;
TRUNCATE TABLE instructors;
TRUNCATE TABLE students;

SET FOREIGN_KEY_CHECKS = 1;

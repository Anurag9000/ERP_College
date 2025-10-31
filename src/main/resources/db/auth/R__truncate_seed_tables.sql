-- Helper script to reset authentication seed data for local development.
-- Run manually when you need a clean slate; this is not executed automatically by Flyway.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE password_history;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

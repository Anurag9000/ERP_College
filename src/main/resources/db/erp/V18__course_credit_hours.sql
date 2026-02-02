-- Add credit_hours to courses table
-- NOTE: This column is now part of V1__init_erp_schema.sql initial schema
-- This migration is kept for backward compatibility with existing databases
ALTER TABLE courses ADD COLUMN IF NOT EXISTS credit_hours INT NOT NULL DEFAULT 3;

-- No UPDATE needed since column now has DEFAULT in initial schema

-- Add feedback column to grades table
-- NOTE: This column is now part of V1__init_erp_schema.sql initial schema
-- This migration is kept for backward compatibility with existing databases
ALTER TABLE grades ADD COLUMN IF NOT EXISTS feedback TEXT AFTER score;

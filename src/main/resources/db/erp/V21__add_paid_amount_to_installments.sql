-- Add paid_amount to fee_installments to track partial payments
-- NOTE: This column is now part of V6__finance_tables.sql initial schema
-- This migration is kept for backward compatibility with existing databases
ALTER TABLE fee_installments ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.0 AFTER amount;

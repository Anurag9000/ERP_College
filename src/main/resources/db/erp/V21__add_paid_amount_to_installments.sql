-- Add paid_amount to fee_installments to track partial payments
ALTER TABLE fee_installments ADD COLUMN paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.0 AFTER amount;

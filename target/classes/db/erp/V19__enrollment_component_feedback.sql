-- Add feedback column to grades table
ALTER TABLE grades ADD COLUMN feedback TEXT AFTER score;

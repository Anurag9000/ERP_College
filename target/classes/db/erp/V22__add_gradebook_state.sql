-- Add gradebook_state to sections
ALTER TABLE sections ADD COLUMN gradebook_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

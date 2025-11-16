ALTER TABLE section_waitlist
    ADD COLUMN advisor_approved TINYINT(1) NOT NULL DEFAULT 0 AFTER position;

-- Add index to audit_events for faster history lookups by user
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor);

-- Add composite index to notifications for faster dashboard loading (filtering by target and sorting by date)
CREATE INDEX IF NOT EXISTS idx_notifications_target_created ON notifications(target_id, created_at);

-- Add index to students for faster status filtering (e.g. Active students)
CREATE INDEX IF NOT EXISTS idx_students_status ON students(status);

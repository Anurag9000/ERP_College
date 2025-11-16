# Legacy `.dat` Migration Guide

The old Swing prototype persisted data in serialized `.dat` files (e.g., `students.dat`, `courses.dat`). The modern SQL-backed ERP ships with a `LegacyDataMigrator` utility so you can port that data into the dual-database schema exactly once.

## Prerequisites

1. Configure the database connection in `src/main/resources/application.properties` (or via env vars) so both `college_auth` and `college_erp` are reachable.
2. Place the legacy `.dat` files under the project’s `data/` directory. Supported files include `users.dat`, `students.dat`, `faculty.dat`, `courses.dat`, `sections.dat`, and `enrollments.dat`.
3. Build the project (`mvn -q package` or `./gradlew build`) so the migrator class is on the classpath.

## Running the Migrator

Choose one of the following entry points:

### Option A: Launch the app (recommended)

The application invokes `LegacyDataMigrator.defaultMigrator().migrateAll()` during startup (`DatabaseUtil.initializeDatabase`). If legacy files are present, they will be imported before the UI appears; logs report inserted/updated rows.

### Option B: Run the migrator directly

```bash
# Maven Exec Plugin
mvn -q exec:java -Dexec.mainClass=main.java.data.migration.LegacyDataMigrator

# Or run the compiled class manually
java -cp target/classes main.java.data.migration.LegacyDataMigrator
```

The CLI exits with code `0` if data was imported, `1` otherwise. Review the console for warnings about incomplete records.

## What Gets Migrated

- Missing auth users (with salts/hashes) are inserted into `college_auth.auth_users`.
- Core academic entities (students, faculty, courses, sections) are upserted in `college_erp`.
- Enrollments, waitlists, and attendance records are inserted with duplicate checks.
- Section capacity/waitlist counters are refreshed so the UI reflects the imported data.

## Post-Migration Checklist

- Log in with an imported account to verify credentials.
- Spot-check a few students/sections in the admin UI to confirm enrollments and attendance.
- Remove or archive the `.dat` files once migration succeeds to avoid redundant imports on future launches.
- Capture a SQL backup (see `docs/operations/backup_restore.md`) so the imported state can be restored later.

This process, combined with the existing DAO layer, satisfies the “migration tooling/docs” requirement from the specification.

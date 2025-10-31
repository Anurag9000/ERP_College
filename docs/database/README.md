# Database Guide

This project splits persistence responsibilities between two schemas:

| Schema | Purpose | Migration Path |
| --- | --- | --- |
| `college_auth` | Authentication users, password history, lockouts, audit metadata | `src/main/resources/db/auth` |
| `college_erp` | Academic entities (students, instructors, sections, enrollments, grades, attendance, notifications, settings, finance) | `src/main/resources/db/erp` |

## Provisioning & Seeding

1. Create two empty databases in MariaDB or MySQL Community:
   ```sql
   CREATE DATABASE college_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE DATABASE college_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Update `src/main/resources/application.properties` with credentials that have DDL privileges on both schemas.
3. Run migrations:
   ```bash
   mvn -q flyway:migrate -Dflyway.configFiles=src/main/resources/flyway-auth.conf
   mvn -q flyway:migrate -Dflyway.configFiles=src/main/resources/flyway-erp.conf
   ```
   (Alternatively, run the application entry point — `Main` bootstraps Flyway automatically.)
4. Seeds:
   - Auth schema creates the default `admin/admin123`, `inst1/inst123`, `stu1/stud123`, and `stu2/stud456` accounts.
   - ERP schema seeds canonical courses, students, instructors, sections, enrollments, waitlists, notifications, and settings.

## Rollback & Reset Strategy

Flyway Community edition does not support automatic undo migrations. Use one of the following approaches:

1. **Snapshot back-up:** take a schema dump before testing:
   ```bash
   mysqldump college_auth > backup/college_auth.sql
   mysqldump college_erp  > backup/college_erp.sql
   ```
   Restore via `mysql college_auth < backup/college_auth.sql`.

2. **Seed reset helpers:** run the truncate helpers shipped in `R__truncate_seed_tables.sql` (auth + ERP) to clear seeded tables while retaining migration history:
   ```sql
   -- In college_auth
   SOURCE src/main/resources/db/auth/R__truncate_seed_tables.sql;

   -- In college_erp
   SOURCE src/main/resources/db/erp/R__truncate_seed_tables.sql;
   ```
   After truncation, re-run `flyway migrate` to reinsert seed data.

3. **Targeted downgrade:** run `flyway repair` followed by `flyway -target=1` (or the desired lower version) to migrate back; note that this only works safely if newer migrations are idempotent.

Document every destructive operation you perform in `docs/CHANGELOG.md` (to be created) so QA can reproduce the database state during acceptance testing.

## Legacy `.dat` Migration

The one-time helper `main.java.data.migration.LegacyDataMigrator` imports existing serialized `.dat` payloads into SQL. Invoke it by running the application once after copying legacy files into the `data/` directory. The migrator will:

- Insert any missing users, courses, students, sections, enrollments, and attendance rows.
- Rebuild waitlists and course availability counts.

Review logs for warnings about incomplete records. After successful migration, remove or archive the `.dat` files.

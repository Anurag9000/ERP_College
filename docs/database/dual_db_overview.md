# Dual Database Implementation Overview

This application ships with two distinct schemas so authentication/security data can be isolated from the main ERP domain. This document pulls together everything an operator/reviewer needs to understand the split, the DDL sources, rollback strategy, and the seed/loader utilities.

## 1. Schema at a Glance

| Schema        | Purpose                                             | Primary Tables (non-exhaustive)                              |
|---------------|-----------------------------------------------------|--------------------------------------------------------------|
| `college_auth`| Users, password history, failed-attempt counters, audit metadata | `auth_users`, `password_history`, `login_attempts`, `audit_log` |
| `college_erp` | Academic entities, transactional data, notifications, finance | `students`, `faculty`, `courses`, `sections`, `enrollments`, `attendance`, `notifications`, `settings`, `fee_installments`, `payment_transactions` |

> Full ERDs live in `docs/diagrams/` (see `docs/diagrams/README.md`). They mirror the Flyway migrations described below.

## 2. Source of Truth for DDL

- Auth schema migrations: `src/main/resources/db/auth/V1__init_auth_schema.sql` (baseline) and `V2__seed_users.sql` for seed data. The repeatable script `R__truncate_seed_tables.sql` resets auth tables to the seed state.
- ERP schema migrations: `src/main/resources/db/erp/V1__init_erp_schema.sql` through `V6__finance_tables.sql` plus repeatable `R__truncate_seed_tables.sql` for reseting demo data.
- Flyway configuration files (`src/main/resources/flyway-auth.conf`, `flyway-erp.conf`) define datasource URLs, placeholders, and migration paths.

## 3. Deployment Flow

1. Provision the schemas (see `docs/database/README.md` for SQL snippets).
2. Run Flyway migrations for each schema (either via `mvn flyway:migrate` or by launching the application, which calls `DatabaseBootstrap.migrate()` during startup).
3. Verify the default seed users:
   - `admin/admin123`
   - `inst1/inst123`
   - `stu1/stud123`
   - `stu2/stud456`
4. (Optional) Use `LegacyDataMigrator` (`main.java.data.migration.LegacyDataMigrator`) to import legacy `.dat` payloads once the database connection details are configured.

## 4. Rollback & Recovery

- **Snapshot dumps:** Use the provided `scripts/db-backup.sh|ps1` and `db-restore.sh|ps1` wrappers (documented in `docs/operations/backup_restore.md`) to capture schema dumps before risky operations.
- **Seed resets:** The repeatable `R__truncate_seed_tables.sql` files clear seed rows while preserving Flyway history so migrations can reapply cleanly.
- **Targeted Flyway downgrade:** When migrations are idempotent, operators can run `flyway repair` followed by `flyway migrate -target=<version>` to step back to a prior schema version.

## 5. Seed & Loader Utilities

- `LegacyDataMigrator` scans the `data/` directory for serialized `.dat` payloads and inserts missing students, faculty, sections, enrollments, and attendance data into the SQL layer.
- `DatabaseUtil.initializeDatabase()` loads demo data via DAOs if seed tables are empty.
- DAO classes live in `src/main/java/main/java/data/dao/` and are the single point of truth for CRUD operations, ensuring both schemas remain in sync with in-memory caches.

## 6. Operational Checklist

- [ ] Maintain ER diagrams alongside Flyway changes.
- [ ] Capture backups before running ad-hoc ALTER/DELETE scripts.
- [ ] Update this document whenever new schemas or critical tables are introduced.
- [ ] Keep Flyway configs (`flyway-auth.conf`, `flyway-erp.conf`) in sync with production credentials (never commit secrets).

With this document plus the existing `docs/database/README.md`, the dual-schema architecture is fully described for auditors, operators, and developers.

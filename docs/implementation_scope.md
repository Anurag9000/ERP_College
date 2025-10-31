# ERP Modernization Implementation Plan

This document captures the exhaustive workflow required to satisfy the complete project brief in `Documentation/project.pdf` alongside the beginner/intermediate enhancements we agreed upon. It is organized into executable workstreams so we can deliver the solution in focused chunks.

## 1. Platform & Architecture Foundation
- **Status:** ✅ Core tooling wired (Maven deps, config loader, datasource registry, Flyway bootstrap).
- Adopt freeware stack (OpenJDK, Swing, JDBC/HikariCP, MariaDB/PostgreSQL, Flyway/Liquibase, FlatLaf, PDFBox/OpenPDF, Commons CSV, SLF4J/Logback, JUnit/Mockito).
- Split persistence into **Auth DB** and **ERP DB**; design ER diagrams, DDL, migration scripts, seed data, rollback plan.
- Introduce modular package layout (`data`, `service`, `api`, `ui`) per spec; configure dependency injection / service locators as needed.

## 2. Authentication & Security
- **Status:** ✅ Password policy, lockouts, audit logging, change/reset UX delivered.
- PBKDF2/BCrypt hashing with per-user salts, password complexity policy, password history, configurable lockout.
- Implement change-password UI (student/instructor) and admin reset flow with forced change.
- Session timeout with auto-logoff banner; audit trail logging for logins, maintenance toggles, enrollment/grade operations.

## 3. Data Migration & Repositories
- **Status:** 🟡 Auth DB migrated to SQL (Flyway schema, DAO layer, runtime integration). ERP entities still backed by file storage pending migration.
- Replace file-based `DatabaseUtil` with DAO/repository layer backed by SQL.
- Provide migration scripts to port existing `.dat` seed data into SQL tables.
- Implement connection pooling (HikariCP), configuration management, health checks.

## 4. Student Experience Enhancements
- Catalog search with filters, prerequisite/co-/anti-requisite enforcement, credit caps, advisor approvals.
- Registration workflow respecting deadlines, waitlists, clashes, notifications; timetable grid + PDF/ICS export.
- Grade view with component breakdown, GPA progression, academic standing alerts; transcript PDF/CSV.
- Financial dashboard: fee schedule, payment history, outstanding reminders.
- Notification inbox with categories and maintenance alerts.

## 5. Instructor Workspace Enhancements
- Section overview, attendance capture (manual + CSV), gradebook editor with rubrics, feedback comments.
- Assessment weighting templates, grade import/export, publish/finalize toggles, moderation workflow.
- Analytics widgets (grade distribution charts, attendance metrics); messaging hub for enrolled students.

## 6. Admin Console & Operations
- User lifecycle management linked to Auth DB; bulk import/export utilities (CSV/XLS).
- Course/catalog/section management with prerequisites, capacity, room scheduling.
- Enrollment oversight (approvals, overrides, deadline adjustments, waitlist control).
- Maintenance scheduler (immediate + future windows) with auto notifications and system-wide read-only enforcement.
- Backup/restore tooling wrapping mysqldump/pg_dump, archival, anonymization utilities.
- Reporting suite: enrollment trends, waitlist stats, attendance compliance, financial arrears.

## 7. Maintenance & Notifications
- Central guard preventing writes for non-admins during maintenance; live banners across UIs.
- Notification center with email/SMS stubs, digest, per-user history, targeted broadcasts, and maintenance reminders.

## 8. Analytics & Degree Planning (Intermediate Enhancements)
- Degree audit planner, advisor dashboards, risk alerts.
- Real-time dashboards (enrollment heatmaps, waitlist pressure) using freeware charting.
- Communication log for advising/issue tracking.

## 9. Testing, CI, and Quality Assets
- Automated tests: unit, integration (DAO/service), UI smoke (AssertJ-Swing), performance sanity.
- Acceptance test suite matching PDF checklist; test data, execution guide, and summary report.
- Configure CI pipeline (GitHub Actions) running tests, linting, packaging; logback configuration for audit trails.

## 10. Documentation & Deliverables
- 5–7 page project report (architecture, maintenance, enhancements, testing).
- Diagrams: role use-cases, ERD, component/class, sequence flows.
- How-to-Run manual, environment setup, default credentials, migration steps.
- Demo assets: storyboard, script, slide deck, recorded walkthrough.
- CHANGELOG, contribution guidelines, license notices for all dependencies.

## 11. Deployment & Packaging
- Maven/Gradle build with profiles, runnable fat JAR, externalized config, sample `.env`.
- Docker Compose for community database images, sample data, and smoke test automation.

## Execution Strategy
1. **SQL Foundation** – schema design, connection layer, migration of `DatabaseUtil`.
2. **Auth/Security Enhancements** – password flows, lockouts, audit logging.
3. **Role UIs** – student portal overhaul, instructor workspace, admin console.
4. **Maintenance & Notifications** – scheduling, enforcement, messaging center.
5. **Analytics & Reporting** – charts, dashboards, exports.
6. **Testing & Docs** – acceptance suites, documentation, packaging.

We will iterate through these phases, validating against the spec and enhancement checklist at every milestone.

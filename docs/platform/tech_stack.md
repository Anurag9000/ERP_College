# Platform Tech Stack

This project is intentionally composed of permissive, freeware tooling so the ERP can be distributed without commercial licence entanglements. The table below captures the primary runtime, libraries, and supporting utilities. Keep this list in sync with `pom.xml` and any third-party binaries bundled with the application.

| Layer | Technology | Purpose | Licence |
| --- | --- | --- | --- |
| Runtime | OpenJDK 17, Swing | Desktop JVM and UI toolkit | GPL + Classpath Exception |
| UI | FlatLaf 3.5 | Modern look & feel for Swing | Apache-2.0 |
| Persistence | HikariCP 5.1, MariaDB JDBC 3.3, Flyway 10.x | Connection pooling, JDBC driver, schema migrations | Apache-2.0 / LGPL-2.1 / Apache-2.0 |
| Database Engines | MariaDB Community, PostgreSQL (optional) | Primary data stores (auth + ERP schemas) | GPL 2.0 / PostgreSQL |
| Reporting & Export | Apache PDFBox 2.0, Apache Commons CSV 1.11 | Transcript PDF generation, CSV imports/exports | Apache-2.0 |
| Logging | SLF4J 2.0, Logback Classic 1.5 | Structured application logging | MIT / EPL-1.0 & LGPL-2.1 dual |
| Security | PBKDF2 via built-in Java, BCrypt (planned) | Password hashing with per-user salts | GPL + Classpath |
| Testing | JUnit 5.10, Mockito 5.12, AssertJ 3.26 | Unit + integration testing | EPL-2.0, MIT, Apache-2.0 |
| Build | Maven 3.9, npm helper scripts | Dependency & build orchestration | Apache-2.0, MIT |
| Analytics & Charts (planned) | XChart / JFreeChart | Grade & attendance visuals | Apache-2.0 / LGPL-2.0 |

## Schema Split

- **Auth DB (`college_auth`)** — Users, salts/hashes, password history, lockouts, audit metadata. Managed by Flyway migrations in `src/main/resources/db/auth`.
- **ERP DB (`college_erp`)** — Students, instructors, courses, sections, enrollments, grades, attendance, notifications, settings, financial artefacts. Managed by Flyway migrations in `src/main/resources/db/erp`.

## Tooling Notes

- All dependencies in `pom.xml` are Apache/MIT/EPL or equivalent permissive licences.
- Additional freeware candidates (e.g., OpenPDF, Liquibase Community, Spring Boot free tier) can be added here once adopted.
- Document frontend/mobile API adapters under a separate heading when the optional REST layer (Javalin / Spring Boot) is introduced.

# College ERP Platform

This repository hosts a freeware-first Java Swing ERP platform. The codebase has been modernised for SQL persistence, modular DAOs, Flyway-managed migrations, and end-to-end tooling that can be shared without licence friction.

## Freeware Tech Stack

| Concern | Library / Tool | Licence |
| --- | --- | --- |
| Runtime & UI | OpenJDK 17, Swing, FlatLaf | GPL+Classpath, GPL+Classpath, Apache-2.0 |
| Persistence | HikariCP, MariaDB JDBC, Flyway Community | Apache-2.0, LGPL-2.1, Apache-2.0 |
| Reporting / Export | Apache PDFBox, Apache Commons CSV | Apache-2.0, Apache-2.0 |
| Logging | SLF4J 2.x, Logback Classic 1.5 | MIT, EPL-1.0 / LGPL-2.1 dual |
| Testing | JUnit 5, Mockito 5, AssertJ 3 | EPL-2.0, MIT, Apache-2.0 |

**Schemas:** Authentication data lives in `college_auth`; academic operations live in `college_erp`. Both are provisioned via Flyway migrations (`src/main/resources/db/auth` and `src/main/resources/db/erp`). Rollback and seeding steps are described in `docs/database/README.md`.

## Build & Run (Option A — plain JDK)

```bash
# from repo root
javac -d classes $(find src/main/java -name "*.java")
java -cp classes main.java.Main
```

On Windows (PowerShell):

```powershell
Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName | Set-Content sources.txt
javac -d classes @sources.txt
java -cp classes main.java.Main
```

## Build & Run (Option B — Node helper)

```bash
npm run build-java
npm run run-java
```

## Build & Run (Option C — Maven)

```bash
mvn -q -DskipTests exec:java
```

## Notes

- Application state is stored in MariaDB via the DAO layer; legacy `.dat` files can be migrated once via `LegacyDataMigrator`.
- Default login: **admin / admin123**
- Detailed platform documentation lives under `docs/`:
  - `docs/platform/tech_stack.md` — dependency catalogue and architecture notes.
  - `docs/database/README.md` — schema overview, seeding instructions, rollback playbook.
  - `docs/diagrams/` — source files for ERD and component diagrams (see doc for formats).

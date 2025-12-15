# System Architecture

## Overview
The College ERP is a **Java Swing Desktop Application** designed for academic management. It employs a **Monolithic Layered Architecture** with a centralized facade for data storage and modular business logic services. It is designed to be self-contained, using embedded databases or local SQL instances (MariaDB) and file-based persistence for legacy migrations.

## Tech Stack
-   **Language:** Java 17 (OpenJDK)
-   **UI:** Swing + FlatLaf (Pastel Theme Customization)
-   **Database:** MariaDB (JDBC)
-   **Connection Pooling:** HikariCP
-   **Migrations:** Flyway
-   **Logging:** SLF4J + Logback

## High-Level Design

### 1. Presentation Layer (GUI)
Located in `src/main/java/main/java/gui/`.
-   **MainFrame:** The primary container. Manages navigation and session state.
-   **Panels:** Modular views (e.g., `StudentSelfServicePanel`, `SectionPanel`).
-   **Dialogs:** Modal interactions (e.g., `LoginFrame`, `ChangePasswordDialog`).
-   **Style System:** `PastelTheme` and `JCard` provide a unified, modern aesthetic (Pastel UI).

### 2. Service Layer
Located in `src/main/java/main/java/service/`.
-   Encapsulates business logic.
-   **StudentService:** Read-only student data (Profile, Schedule).
-   **EnrollmentService:** Transactional logic for Register/Drop (Prerequisites, Deadlines).
-   **GradebookService:** Instructor logic for grading and stats.
-   **AdminService:** Global settings and user management.

### 3. Data Access Layer (DAO)
Located in `src/main/java/main/java/data/dao/`.
-   Direct SQL interactions.
-   **AuthUserDao:** Security & Credentials.
-   **StudentDao, CourseDao, SectionDao:** Core ERP entities.
-   **AuditLogDao:** centralized auditing.

### 4. The "Facade" (`DatabaseUtil`)
Located in `src/main/java/main/java/utils/DatabaseUtil.java`.
-   **Role:** Acts as the central "Gateway" or "God Class" for the application.
-   **Responsibilities:**
    -   Initializes connections.
    -   Caches frequently accessed data (Course Catalog).
    -   Delegates specific actions to DAOs.
    -   Manages transaction boundaries (implicit via JDBC auto-commit state in some flows).

## Key Workflows

### Authentication
1.  User enters credentials in `LoginFrame`.
2.  `DatabaseUtil.authenticateUser()` calls `AuthUserDao`.
3.  Password salt/hash verified (PBKDF2).
4.  On success, `MainFrame` launches with role-specific panels.

### Student Registration
1.  User clicks "Register" in `StudentSelfServicePanel`.
2.  `EnrollmentService.registerStudent()` is invoked.
3.  **Checks:**
    -   Maintenance Mode?
    -   Already enrolled?
    -   Prerequisites met?
    -   Section capacity? (Waitlist logic if full).
4.  If valid, `EnrollmentDao.insert()` is called.
5.  Audit log entry recorded via `AuditLogService`.

### Smart Calendar
1.  `CalendarPanel` initializes.
2.  Fetches `List<Section>` via `StudentService.getSchedule()`.
3.  Iterates through the current month.
4.  Maps `Section.DayOfWeek` to concrete dates.
5.  Renders events on the grid.

## Database Design
Two separate schemas are used (Dual-DB Pattern):
1.  **`college_auth`:** Users, Roles, Passwords, Login History.
2.  **`college_erp`:** Students, Faculty, Courses, Grades, Finance.

This separation allows strictly isolated security credentials for authentication vs. operational data.

## Future Scalability
-   **Microservices:** The DAO layer can be peeled off into standalone services.
-   **Web Client:** The DB schema is standard SQL, allowing a REST API wrapper (Spring Boot) to coexist with the Swing client.

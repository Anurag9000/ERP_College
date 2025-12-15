# College ERP Platform (v3.0)

A comprehensive, freeware-first Java Swing ERP platform for academic management with **Pastel UI**, **Smart Calendar**, **Faculty Interaction**, **Advanced Analytics**, and **Student-Centric Features**.

## 🌟 Key Features (v3.0)

### Student Experience
- **Smart Calendar:** Auto-generated timetable with personal event tracking
- **Assignments & Tests:** Submission portal with deadline tracking
- **Announcements Hub:** Category-based announcements with opt-in subscriptions
- **Faculty Connect:** "Where's My Prof" locator + appointment booking
- **Grades Dashboard:** Real-time marks tracking with CGPA calculator
- **Examination Portal:** Form submission, admit cards, mark sheets

### Faculty Tools
- **Gradebook Management:** Assessment definition, grade entry, statistics
- **Attendance Tracking:** CSV import/export, bulk operations
- **Assignment Grading:** File submissions with feedback
- **Office Hours:** Publish availability, manage appointments
- **Messaging Hub:** Broadcast to enrolled sections

### Admin Console
- **Advanced Reporting:** 5 analytics dashboards (enrollment, waitlist, attendance, finance, grades)
- **Bulk Operations:** CSV import/export for students, faculty, schedules
- **Data Governance:** Automated backup/restore, archival
- **User Management:** CRUD, role assignment, audit trails
- **Maintenance Scheduler:** Scheduled windows with auto-notifications

### Technical Features
- **Pastel Design System:** Modern, clean UI with soft colors
- **Dual-DB Architecture:** Separate auth and ERP databases
- **Flyway Migrations:** Automated schema versioning
- **Audit Logging:** Comprehensive security event tracking
- **Role-Based Access:** Student/Faculty/Admin dashboards

## 🚀 Getting Started

**Quick Launch:**
```bash
mvn clean compile exec:java
```

**Detailed Instructions:** See [HOW_TO_RUN.md](HOW_TO_RUN.md)

## 📚 Documentation

- **[System Architecture](docs/ARCHITECTURE.md)** - Technical deep dive
- **[Database Schema](docs/database/README.md)** - Schema details
- **[Feature Inventory](docs/feature_inventory.txt)** - Complete feature list

## 🛠 Tech Stack

| Component | Technology |
| :--- | :--- |
| **Language** | Java 17 (OpenJDK) |
| **UI** | Swing + FlatLaf (Pastel Theme) |
| **Database** | MariaDB (HikariCP) |
| **Migrations** | Flyway |
| **Reporting** | Apache PDFBox, Commons CSV |
| **Testing** | JUnit 5, Mockito |

## 🎨 New in v3.0

- ✨ Advanced Reporting Dashboard
- ✨ Bulk Import/Export System
- ✨ Assignments & Tests Module
- ✨ Announcements Hub
- ✨ Examination Workflow
- ✨ Backup/Restore Automation

## Default Credentials

| Role | Username | Password |
| :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` |
| **Student** | `stu1` | `pass123` |
| **Faculty** | `prof1` | `pass123` |

## 📝 License

All dependencies are freeware/open-source (Apache 2.0, MIT, GPL+Classpath, EPL).

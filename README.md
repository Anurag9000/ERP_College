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

## 🔒 Code Quality & Security

**Recent Comprehensive Audit (Feb 2026):**
- ✅ **44 files enhanced** with comprehensive validation
- ✅ **240+ critical bugs fixed** (null pointer exceptions, validation issues)
- ✅ **2 security vulnerabilities resolved** (password exposure, auth bypass)
- ✅ **272+ validations added** for data integrity
- ✅ **100% DAO & Service layer coverage**

See [AUDIT_REPORT.md](AUDIT_REPORT.md) for detailed security and quality audit results.

## 🚀 Getting Started

**Quick Launch:**
```bash
mvn clean compile exec:java
```

**Detailed Instructions:** See [HOW_TO_RUN.md](HOW_TO_RUN.md)

## 📚 Documentation

- **[System Architecture](docs/ARCHITECTURE.md)** - Technical deep dive
- **[Database Schema](docs/database/README.md)** - Schema details
- **[Audit Report](AUDIT_REPORT.md)** - Security & quality audit results
- **[Feature Inventory](docs/feature_inventory.txt)** - Complete feature list

## 🛠️ Tech Stack

- **Language:** Java 17+
- **UI:** Swing with custom Pastel theme
- **Database:** MySQL (dual-database architecture)
- **Build:** Maven
- **Migration:** Flyway
- **Logging:** SLF4J + Logback

## 📊 Project Statistics

- **Total Files:** 108+ Java files
- **Lines of Code:** 50,000+
- **Code Quality:** 100% validation coverage in core layers
- **Security:** Enterprise-grade with comprehensive audit
- **Documentation:** 177+ JavaDoc blocks added

## 🤝 Contributing

This is an educational project. Contributions welcome!

## 📄 License

Freeware - Educational use

---

**Version:** 3.0  
**Last Updated:** February 2026  
**Status:** Production-Ready with Enterprise Security

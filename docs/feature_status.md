# Feature Implementation Tracker

This checklist tracks every requirement from `Documentation/project.pdf` plus the enhancement backlog we identified. Items marked with `[x]` are delivered; `[ ]` items still need implementation.

## Baseline Specification

- [x] Role-based login launches dashboard (Student, Instructor, Admin).
- [x] Tables/search for catalog and section lists.
- [x] Student catalog browse + basic registration/drop UI.
- [x] Grade entry, weighting, and simple stats for instructors.
- [x] Transcript export (CSV/PDF) and student grade view.
- [x] Admin can add users, create courses/sections.
- [x] Admin UI to assign instructors to sections and toggle Maintenance Mode in-app.
- [x] Maintenance mode enforcement for *all* write actions (currently only partial guards).
- [ ] Student drop deadlines strictly enforced.
- [ ] Timetable rendered on a day/time grid and richer printable layout.
- [ ] Instructor grade CSV import/export (optional in spec).
- [ ] Optional backup/restore tooling (spec optional but outstanding).

## Platform & Architecture

- [ ] Document the dual database split (ERDs, full DDL, rollback plan, seed loaders).
- [ ] Modularise packages (`data/`, `service/`, `ui/`, `api/`) and introduce DI/service locator instead of direct `DatabaseUtil` calls.
- [ ] Add health checks/configuration diagnostics (datasource ping, config validation).

## Authentication & Security

- [x] Student/instructor self-service change password screen.
- [x] Admin password reset with "must change next login".
- [x] Extend audit trail (maintenance toggles, enrollment overrides, grade edits) and provide export UI.

## Data Migration & Repositories

- [x] Move enrollment/grade/attendance/waitlist/settings to DAO-backed persistence.
- [ ] Migration tooling/docs to port legacy `.dat` contents into SQL.
- [ ] Datasource health probes + robust DAO error handling.

## Student Experience

- [ ] Registration workflow enhancements:
  - [ ] Clash detection for time **and** room.
  - [ ] Automated waitlist promotion queue with advisor approvals.
  - [ ] Hard enforcement of enrollment/drop deadlines.
  - [ ] Full co-/anti-requisite enforcement.
  - [ ] Departmental/advisor approval flow.
- [ ] Timetable grid visualization + improved printable/PDF output.
- [ ] GPA/standing analytics with historical trend graphs and probation alerts.
- [ ] Transcript watermarking, certificate downloads, registrar messaging workflow.
- [ ] Fee schedule configuration UI, installment visualization for students, polished reminder messaging.
- [ ] Notification inbox upgrades (category filters tied to maintenance/system broadcasts).

## Instructor Workspace

- [ ] Attendance CSV import/export, bulk updates, tardiness tracking, analytics dashboard.
- [ ] Gradebook enhancements: assessment templates, grade import/export, moderation workflow, publish/finalise toggles, inline rubric/feedback entry.
- [ ] Analytics widgets (grade distributions, pass/fail counts, attendance metrics with charts).
- [ ] Messaging hub to reach enrolled students/sections.
- [ ] Section planner (room clash/capacity warnings).

## Admin Console & Operations

- [ ] Full user lifecycle UI (create/assign roles, suspend/reactivate, audit reset events).
- [ ] Catalog management for courses/sections/rooms/prereqs/capacity planning.
- [ ] Enrollment oversight tools (approvals, overrides, extensions, waitlist management).
- [ ] Maintenance scheduler (immediate + future windows, countdown banner, auto notifications).
- [ ] Data governance: backup/restore wrappers, archival, anonymisation scripts + docs.
- [ ] Reporting suite (enrollment trends, waitlist pressure, attendance compliance, financial arrears).
- [ ] Bulk CSV/XLS import/export for students, instructors, courses, enrollments.

## Maintenance & Notifications

- [ ] Central maintenance guard covering every write path plus future scheduling flows.
- [ ] Notification centre enhancements: targeted broadcasts, email/SMS stubs, digest configuration, admin history view.

## Analytics & Degree Planning (Enhancements)

- [ ] Degree audit planner, advisor dashboards, risk alerts.
- [ ] Real-time analytics (heatmaps, waitlist pressure) via freeware charting.
- [ ] Advising communication log / issue tracker linked to student profiles.

## Testing, CI, Quality

- [ ] Automated unit/integration tests for DAO/service layers and UI smoke tests.
- [ ] Acceptance checklist covering login roles, registration, waitlist, maintenance, grade entry, transcripts, finance exports.
- [ ] Load/performance sanity testing (large catalog/student cohort).
- [ ] CI pipeline (GitHub Actions or similar) running migrations, tests, packaging.
- [ ] Logging/audit configuration review (Logback tuning, retention policy).

## Documentation & Deliverables

- [ ] Architecture/maintenance/enhancement/testing report (5–7 pages).
- [ ] Diagram set (use-case, ERD, component/class, sequence diagrams).
- [ ] How-to-run guide, environment setup, default credentials, migration instructions.
- [ ] Demo assets (storyboard, script, slides, recorded walkthrough).
- [ ] CHANGELOG, contribution guidelines, dependency license notices.

## Deployment & Packaging

- [ ] Maven/Gradle profiles, fat JAR, externalised config, sample `.env`.
- [ ] Docker Compose (DB + app), sample data seed, smoke-test automation.
- [ ] Optional REST layer (Spring Boot/Javalin) for integrations/mobile clients.

## Additional Enhancements

- [ ] Public API surface for integrations/mobile apps.
- [ ] CSV/XLS importers for students/courses/enrollments/grades/attendance with validation & rollback.
- [ ] Additional exports (gradebooks, schedules, instructor financial statements).
- [ ] Advisor approval workflow tied into registration UI + backend.

## Student-Centric Calendar & UX Backlog

- [ ] **Central Calendar Hub**: single source of truth for classes/assignments/events with auto-filled academic items, pastel color coding, and student-controlled event additions.
- [ ] **Admin-driven Timetable Publishing**: admins upload per-semester timetables; faculty/students consume read-only latest versions (with cross-department toggle).
- [ ] **Assignments & Tests Module**: subject-wise dashboards, deadlines/reminders, uploads, and marks breakdowns linked to internals.
- [ ] **Attendance & Internal Marks View**: attendance tracking tied into internal mark computation with component-wise visibility.
- [ ] **Examination Window**: consolidated exam form/fee/admit/syllabus/datesheet/marksheet flow.
- [ ] **Announcements Hub**: categorized (dept, union, college, university, society) announcements with opt-in calendar additions and noise controls.
- [ ] **Faculty Communication Flow**: appointment/time-slot requests (no casual chat), TA routing, contact info for formal comms.
- [ ] **Professor Profile Visibility**: quick view of professor schedules (“Where’s my prof?”).
- [ ] **Color-Coding System**: consistent pastel palette per subject across calendar, assignments, marks, and faculty references.
- [ ] **Notification Strategy**: assignment/test reminders, minimal event nudges, user-level preferences to avoid spam.
- [ ] **Grades Tracking Enhancements**: per-assignment/test marks, SGPA/CGPA trackers, course-wise breakdown.
- [ ] **Courses Overview & Planner**: current courses summary with shortcuts plus weekly/monthly planner derived from calendar/tasks.
- [ ] **Role-based Dashboards Refresh**: refined student/prof/admin dashboards reflecting the new modules.
- [ ] **Department Visibility Controls**: default relevant views with ability to peek into other departments deliberately.
- [ ] **Security/Compliance Messaging**: address data privacy/government safety concerns for adoption.
- [ ] **UI/UX Benchmark**: enforce clean, minimal, intuitive navigation with color cues > text clutter.
- [ ] **Optional Google Classroom Sync**: assignment sync where institutes already use GC.
- [ ] **Meeting Slots**: students request meetings; professors approve/offer slots via their calendar.
- [ ] **Manual Event Philosophy**: campus events only appear if students opt-in; no auto flooding.
- [ ] **Booking Tutor/TA Support**: structured channel for TA-routed questions.
- [ ] **Exam Artefacts Together**: ensure all exam artefacts remain in a single UX flow.
- [ ] **App/Web Balance**: plan for high-frequency tasks in app, low-touch via web surfaces.
- [ ] **Admin/Prof Controls**: only admins (or authorized profs) edit timetables/course artefacts; others view-only.
- [ ] **Announcements Safety Rails**: categories drive notification priority to keep noise low.
- [ ] **Research/Collab Mode**: structured connect workflow for students working with professors/PhDs.
- [ ] **Personalisation Defaults**: calendar pre-loads must-have entries; electives/extras remain opt-in.
- [ ] **Cross-department Curiosity**: allow manual subscription to other department events/timetables.
- [ ] **Scalable Architecture Hooks**: configurable categories/roles/mark schemes for institution-to-institution variance.
- [ ] **Editable Timetable Windows**: admin-level ability to edit timetable entries for demos/vivas/assignments and show instant impact.
- [ ] **Current Marks Within Course**: display running marks/grades within each course panel.
- [ ] **TA Office Hours & Booking**: assistants publish slots; students request/book structured sessions (with TA delegation route).
- [ ] **Interest Clubs Dashboard**: students follow clubs/societies, get category-specific announcements, and manually add events to calendar.
- [ ] **Timetable Conflict Simulator**: admins can simulate timetable edits and auto-detect clashes before publishing.
- [ ] **Free Slot Finder**: tool for students/professors to compute common free hours across enrolled courses.
- [ ] **External Calendar Sync**: generate iCal feeds per user so classes/tests sync to Google/Apple calendars.
- [ ] **Inline Feedback Threads**: instructors leave per-question feedback on assignments/tests; students acknowledge/respond.
- [ ] **Theme & Accessibility Settings**: high-contrast, font scaling, and color-blind-friendly palettes across the UI.
- [ ] **Preference-based Notifications**: user-level controls over which notification categories and digest frequency to avoid spam.
- [ ] **Personal Learning Goals Dashboard**: students set attendance/grade goals and track progress visually.

🟪 A. Calendar & Planner (Missing Completely)
Central Smart Calendar as the main hub
Auto-fill of classes/tests into the calendar
Student manual event addition (opt-in events)
Pastel/dull subject color coding inside calendar
Dot/label indicators for subjects/events
Weekly/Monthly planner view (Not just timetable)
🟪 B. Visual Design System (Missing Completely)
Pastel UI theme as a strict design rule
Subject color consistency across assignments/marks/notifications
Dot-based subtle color indicators
Minimalistic clean UX design philosophy (“bad UI → no use”)
🟪 C. Event Philosophy (Missing Completely)
Events should NOT auto-dump into the calendar
Only event headlines shown; details only if student adds
Rejecting ‘interest-based auto add’ logic
Strict noise-control philosophy for events
🟪 D. Faculty Interaction & Availability (Missing Completely)
Appointment / slot-booking system (student → professor)
TA-routed academic queries system
Prof Profile window (view prof + subjects + schedule)
“Where’s My Prof now?” quick lookup
Prof availability timeline visible to students
🟪 E. Personalisation & Notifications (Missing Completely)
Per-category notification preferences
Student controls what notifications to receive
Anti-spam rules for notifications
Personalised defaults (classes auto-load; events opt-in)
🟪 F. Department-Level Logic (Missing Completely)
Dept-based visibility defaults (default = own dept)
Toggle to view other departments’ timetables/announcements
Cross-dept curiosity / manual add of other dept events
🟪 G. Rollout, Trust, Architecture Philosophy (Missing Completely)
Institute-first rollout strategy (start with IIITD)
App vs Web usage-frequency strategy
Govt,DU trust, privacy, compliance plan
🟪 H. UX Micro-Details (Missing Completely)
Subtle background color rules for subjects
Non-techie-friendly naming/labels

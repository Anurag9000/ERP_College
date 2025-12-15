# Integration Guide - v3.0 Features

## Quick Integration Steps

### 1. Update StudentSelfServicePanel

Add these imports:
```java
import main.java.gui.panels.AssignmentsPanel;
import main.java.gui.panels.AnnouncementsHubPanel;
```

In the `createBody()` method, add new tabs:
```java
private JComponent createBody() {
    JTabbedPane tabs = new JTabbedPane();
    tabs.setFont(PastelTheme.BODY_FONT);
    tabs.addTab("Smart Calendar", new CalendarPanel(currentUser));
    tabs.addTab("Faculty Connect", new FacultyConnectPanel(currentUser));
    tabs.addTab("Assignments", new AssignmentsPanel(currentUser));  // NEW
    tabs.addTab("Announcements", new AnnouncementsHubPanel(currentUser));  // NEW
    tabs.addTab("Catalog & Registration", buildCatalogTab());
    tabs.addTab("Timetable", buildScheduleTab());
    tabs.addTab("Grades", new JScrollPane(gradesTable));
    tabs.addTab("Finance", buildFinanceTab());
    tabs.addTab("Notifications", buildNotificationsTab());
    tabs.addTab("Transcript", buildTranscriptTab());
    return tabs;
}
```

### 2. Update Admin Dashboard (if MainFrame exists)

Add these imports:
```java
import main.java.gui.panels.ReportsPanel;
import main.java.gui.panels.BulkOperationsPanel;
```

Add menu items or tabs:
```java
// In admin menu/tabs
adminTabs.addTab("Reports & Analytics", new ReportsPanel());
adminTabs.addTab("Bulk Operations", new BulkOperationsPanel());
```

### 3. Database Migration

Ensure Flyway runs these migrations in order:
- V1__init_erp_schema.sql (existing)
- V2__faculty_interaction.sql (appointments, office hours)
- V3__assignments_announcements.sql (assignments, announcements)
- V4__examination_module.sql (exam forms, admit cards)

### 4. Verify Dependencies

Ensure these are in classpath:
- HikariCP (connection pooling)
- MariaDB JDBC driver
- Flyway (migrations)
- Apache PDFBox (for future PDF generation)
- Apache Commons CSV (for bulk operations)

## Testing Checklist

- [ ] Login with student account
- [ ] Verify "Assignments" tab loads
- [ ] Verify "Announcements" tab loads
- [ ] Test assignment submission
- [ ] Test announcement filtering
- [ ] Login with admin account
- [ ] Verify "Reports" panel loads
- [ ] Test CSV export
- [ ] Test backup creation

## Troubleshooting

**Issue:** Tabs don't appear
**Solution:** Ensure imports are correct and panels are instantiated with `currentUser`

**Issue:** Database errors
**Solution:** Run Flyway migrations: `mvn flyway:migrate`

**Issue:** CSV operations fail
**Solution:** Check file permissions in working directory

## Next Steps

1. Run the application
2. Test all new features
3. Review generated reports
4. Test bulk import with sample CSV
5. Create a backup to verify automation

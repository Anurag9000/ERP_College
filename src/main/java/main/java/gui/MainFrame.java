package main.java.gui;

import main.java.models.User;
import main.java.gui.panels.*;
import main.java.gui.panels.ReportsPanel;
import main.java.gui.panels.BulkOperationsPanel;
import javax.swing.*;
import java.awt.*;

/**
 * Main application window for the College ERP.
 * Dynamically switches layouts based on user roles and manages tabbed
 * navigation.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private JTabbedPane tabbedPane;

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("College ERP - " + (user != null ? user.getRole() : "System") + " Portal");
        setSize(1366, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        if (currentUser == null) {
            setupGuestView();
        } else {
            switch (currentUser.getRole().toLowerCase()) {
                case "admin" -> setupAdminView();
                case "instructor" -> setupInstructorView();
                case "student" -> setupStudentView();
                default -> setupGuestView();
            }
        }

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Add a simple status bar at the bottom
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JLabel userLabel = new JLabel("Logged in as: " + (currentUser != null ? currentUser.getFullName() : "Guest"));
        userLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusBar.add(userLabel);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    private void setupAdminView() {
        tabbedPane.addTab("Dashboard", new DashboardPanel());
        tabbedPane.addTab("Students", new StudentPanel());
        tabbedPane.addTab("Faculty", new FacultyPanel());
        tabbedPane.addTab("Courses", new CoursePanel());
        tabbedPane.addTab("Sections", new SectionPanel(currentUser));
        tabbedPane.addTab("Waitlists", new WaitlistApprovalPanel(currentUser));
        tabbedPane.addTab("Registrations", new RegistrationApprovalPanel(currentUser));
        tabbedPane.addTab("Maintenance", new MaintenancePanel(currentUser, () -> {
        }));
        tabbedPane.addTab("Audit Logs", new AuditLogPanel());
        tabbedPane.addTab("Reports & Analytics", new ReportsPanel());
        tabbedPane.addTab("Bulk Operations", new BulkOperationsPanel());
    }

    private void setupInstructorView() {
        tabbedPane.addTab("Instructor Workspace", new InstructorWorkspacePanel(currentUser));
    }

    private void setupStudentView() {
        tabbedPane.addTab("Student Self-Service", new StudentSelfServicePanel(currentUser));
    }

    private void setupGuestView() {
        tabbedPane.addTab("Welcome", new JPanel()); // Placeholder
    }
}

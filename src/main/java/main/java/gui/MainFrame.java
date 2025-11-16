package main.java.gui;

import main.java.config.ConfigLoader;
import main.java.gui.dialogs.ChangePasswordDialog;
import main.java.models.User;
import main.java.gui.panels.*;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main application frame with tabbed interface
 */
public class MainFrame extends JFrame {
    private User currentUser;
    private JTabbedPane tabbedPane;
    private DashboardPanel dashboardPanel;
    private StudentPanel studentPanel;
    private FacultyPanel facultyPanel;
    private CoursePanel coursePanel;
    private FeesPanel feesPanel;
    private SectionPanel sectionPanel;
    private EnrollmentPanel enrollmentPanel;
    private AttendancePanel attendancePanel;
    private NotificationsPanel notificationsPanel;
    private UserManagementPanel userManagementPanel;
    private AuditLogPanel auditLogPanel;
    private JPanel studentSelfServicePanel;
    private JPanel instructorWorkspacePanel;
    private JLabel maintenanceLabel;
    private JLabel sessionCountdownLabel;
    private Timer sessionTimer;
    private long sessionTimeoutMillis;
    private long lastActivity;
    private AWTEventListener activityListener;
    
    public MainFrame(User user) {
        this.currentUser = user;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        initSessionTimeout();
        forcePasswordChangeIfRequired();
        
        setTitle("College ERP System - " + user.getFullName() + " (" + user.getRole() + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 600));
    }
    
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));
        
        initializePanelsForRole();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(37, 99, 235));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel titleLabel = new JLabel("College ERP System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.setOpaque(false);

        JLabel userLabel = new JLabel("Welcome, " + currentUser.getFullName());
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.setBackground(new Color(37, 99, 235).darker());
        changePasswordButton.setForeground(Color.WHITE);
        changePasswordButton.setFocusPainted(false);
        changePasswordButton.setBorderPainted(false);
        changePasswordButton.setPreferredSize(new Dimension(140, 30));
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());

        maintenanceLabel = new JLabel();
        maintenanceLabel.setForeground(Color.YELLOW);
        maintenanceLabel.setFont(new Font("Arial", Font.BOLD, 12));
        updateMaintenanceBadge();

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(220, 38, 38));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setPreferredSize(new Dimension(80, 30));
        logoutButton.addActionListener(e -> logout());

        userPanel.add(userLabel);
        userPanel.add(Box.createHorizontalStrut(10));
        userPanel.add(changePasswordButton);
        userPanel.add(Box.createHorizontalStrut(10));
        userPanel.add(maintenanceLabel);
        userPanel.add(Box.createHorizontalStrut(20));
        userPanel.add(logoutButton);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(userPanel, BorderLayout.EAST);
        
        // Add tabs
        addRoleSpecificTabs();
        
        // Add components to frame
        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        
        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBar.setPreferredSize(new Dimension(0, 25));
        
        JLabel statusLabel = new JLabel(" Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);

        sessionCountdownLabel = new JLabel("");
        sessionCountdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sessionCountdownLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        sessionCountdownLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        statusBar.add(sessionCountdownLabel, BorderLayout.CENTER);
        
        JLabel timeLabel = new JLabel(java.time.LocalDateTime.now().toString());
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusBar.add(timeLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void initializePanelsForRole() {
        String role = currentUser.getRole();
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));

        if ("Student".equalsIgnoreCase(role)) {
            studentSelfServicePanel = new StudentSelfServicePanel(currentUser);
        } else if ("Instructor".equalsIgnoreCase(role)) {
            instructorWorkspacePanel = new InstructorWorkspacePanel(currentUser);
        } else {
            dashboardPanel = new DashboardPanel();
            studentPanel = new StudentPanel();
            facultyPanel = new FacultyPanel();
            coursePanel = new CoursePanel();
            feesPanel = new FeesPanel();
            sectionPanel = new SectionPanel(currentUser);
            enrollmentPanel = new EnrollmentPanel(currentUser);
            attendancePanel = new AttendancePanel();
            notificationsPanel = new NotificationsPanel();
            userManagementPanel = new UserManagementPanel(currentUser);
            auditLogPanel = new AuditLogPanel();
        }
    }

    private void addRoleSpecificTabs() {
        String role = currentUser.getRole();
        if ("Student".equalsIgnoreCase(role)) {
            tabbedPane.addTab("Self Service", createTabIcon("🎓"), studentSelfServicePanel);
        } else if ("Instructor".equalsIgnoreCase(role)) {
            tabbedPane.addTab("Workspace", createTabIcon("📘"), instructorWorkspacePanel);
            tabbedPane.addTab("Attendance", createTabIcon("📝"), new InstructorAttendancePanel(currentUser));
        } else {
            tabbedPane.addTab("Dashboard", createTabIcon("📊"), dashboardPanel);
            tabbedPane.addTab("Users", createTabIcon("👤"), userManagementPanel);
            tabbedPane.addTab("Students", createTabIcon("👥"), studentPanel);
            tabbedPane.addTab("Faculty", createTabIcon("👨‍🏫"), facultyPanel);
            tabbedPane.addTab("Courses", createTabIcon("📚"), coursePanel);
            tabbedPane.addTab("Sections", createTabIcon("🗓️"), sectionPanel);
            tabbedPane.addTab("Enrollment", createTabIcon("✅"), enrollmentPanel);
            tabbedPane.addTab("Attendance", createTabIcon("📝"), attendancePanel);
            tabbedPane.addTab("Fees", createTabIcon("💰"), feesPanel);
            tabbedPane.addTab("Notifications", createTabIcon("🔔"), notificationsPanel);
            tabbedPane.addTab("Maintenance", createTabIcon("🛠"), new MaintenancePanel(currentUser, this::updateMaintenanceBadge));
        }
    }

    private void updateMaintenanceBadge() {
        boolean maintenance = main.java.utils.DatabaseUtil.isMaintenanceMode();
        maintenanceLabel.setText(maintenance ? "Maintenance ON" : "");
        if (studentSelfServicePanel instanceof StudentSelfServicePanel) {
            ((StudentSelfServicePanel) studentSelfServicePanel).refreshForMaintenance();
        }
        if (instructorWorkspacePanel instanceof InstructorWorkspacePanel) {
            ((InstructorWorkspacePanel) instructorWorkspacePanel).updateMaintenanceState();
        }
        notifyMaintenanceAware(maintenance);
    }

    private void notifyMaintenanceAware(boolean maintenance) {
        if (studentPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (facultyPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (coursePanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (feesPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (sectionPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (enrollmentPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (attendancePanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (notificationsPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
        if (userManagementPanel instanceof MaintenanceAware aware) {
            aware.onMaintenanceModeChanged(maintenance);
        }
    }

    private void initSessionTimeout() {
        String minutesConfig = ConfigLoader.getOrDefault("app.session.timeout.minutes", "30");
        long minutes;
        try {
            minutes = Long.parseLong(minutesConfig.trim());
        } catch (NumberFormatException ex) {
            minutes = 30L;
        }
        if (minutes <= 0) {
            sessionTimeoutMillis = 0L;
            clearSessionCountdownLabel();
            return;
        }
        sessionTimeoutMillis = minutes * 60_000L;
        lastActivity = System.currentTimeMillis();
        activityListener = event -> lastActivity = System.currentTimeMillis();
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        sessionTimer = new Timer("SessionTimer", true);
        long period = Math.max(5_000L, Math.min(60_000L, sessionTimeoutMillis / 10));
        sessionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkSessionTimeout();
            }
        }, period, period);
        SwingUtilities.invokeLater(() -> updateSessionCountdownLabel(sessionTimeoutMillis));
    }    private void checkSessionTimeout() {
        if (sessionTimeoutMillis <= 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - lastActivity;
        long remaining = sessionTimeoutMillis - elapsed;
        if (remaining <= 0) {
            disposeSessionTimer();
            SwingUtilities.invokeLater(this::logoutDueToTimeout);
        } else {
            SwingUtilities.invokeLater(() -> updateSessionCountdownLabel(remaining));
        }
    }    private void logoutDueToTimeout() {
        JOptionPane.showMessageDialog(this,
                "Session timed out due to inactivity. Please log in again.",
                "Session Timeout",
                JOptionPane.INFORMATION_MESSAGE);
        performLogout(false, null);
    }

    private void disposeSessionTimer() {
        if (activityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
            activityListener = null;
        }
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer.purge();
            sessionTimer = null;
        }
        SwingUtilities.invokeLater(this::clearSessionCountdownLabel);
    }    private void showChangePasswordDialog() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(this, currentUser.getUsername());
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            currentUser = DatabaseUtil.getUser(currentUser.getUsername());
            JOptionPane.showMessageDialog(this, "Password updated successfully.");
        }
    }

    private void forcePasswordChangeIfRequired() {
        if (currentUser.isMustChangePassword()) {
            SwingUtilities.invokeLater(this::enforceMandatoryPasswordChange);
        }
    }

    private void enforceMandatoryPasswordChange() {
        JOptionPane.showMessageDialog(this,
                "You must change your password before continuing.",
                "Change Password",
                JOptionPane.WARNING_MESSAGE);
        while (currentUser != null && currentUser.isMustChangePassword()) {
            ChangePasswordDialog dialog = new ChangePasswordDialog(this, currentUser.getUsername());
            dialog.setVisible(true);
            if (dialog.isChanged()) {
                JOptionPane.showMessageDialog(this, "Password updated successfully.");
            } else {
                int option = JOptionPane.showConfirmDialog(this,
                        "Password change is required. Exit application?",
                        "Change Password",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION) {
                    performLogout(false, null);
                    return;
                }
            }
        }
    }
    
    private void updateSessionCountdownLabel(long remainingMillis) {
        if (sessionCountdownLabel == null) {
            return;
        }
        if (remainingMillis <= 0) {
            sessionCountdownLabel.setText("Session ending now");
            sessionCountdownLabel.setForeground(new Color(255, 193, 7));
            return;
        }
        long seconds = Math.max(0L, remainingMillis / 1000L);
        long minutes = seconds / 60L;
        long secs = seconds % 60L;
        sessionCountdownLabel.setText(String.format("Session ends in %02d:%02d", minutes, secs));
        sessionCountdownLabel.setForeground(remainingMillis <= 300_000L ? Color.ORANGE : Color.WHITE);
    }

    private void clearSessionCountdownLabel() {
        if (sessionCountdownLabel != null) {
            sessionCountdownLabel.setText("");
            sessionCountdownLabel.setForeground(Color.WHITE);
        }
    }
    private ImageIcon createTabIcon(String emoji) {
        // Simple text-based icon
        return null; // For simplicity, not using icons
    }
    
    private void setupEventHandlers() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(
                    MainFrame.this,
                    "Are you sure you want to exit?",
                    "Exit Application",
                    JOptionPane.YES_NO_OPTION
                );
                if (option == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }
    
    private void logout() {
        performLogout(true, null);
    }

    private void performLogout(boolean requireConfirm, String message) {
        if (requireConfirm) {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to logout?",
                    "Logout",
                    JOptionPane.YES_NO_OPTION
            );
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }
        disposeSessionTimer();
        currentUser = null;
        dispose();
        if (message != null) {
            JOptionPane.showMessageDialog(null, message, "Session", JOptionPane.INFORMATION_MESSAGE);
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}










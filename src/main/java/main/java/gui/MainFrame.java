package main.java.gui;

import main.java.models.User;
import main.java.gui.panels.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
    private JPanel studentSelfServicePanel;
    private JPanel instructorWorkspacePanel;
    private JLabel maintenanceLabel;
    
    public MainFrame(User user) {
        this.currentUser = user;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
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
            sectionPanel = new SectionPanel();
            enrollmentPanel = new EnrollmentPanel();
            attendancePanel = new AttendancePanel();
            notificationsPanel = new NotificationsPanel();
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
        int option = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Logout",
            JOptionPane.YES_NO_OPTION
        );
        if (option == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
            });
        }
    }
}

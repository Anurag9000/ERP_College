package main.java.gui.panels;

import main.java.utils.DatabaseUtil;
import main.java.models.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collection;

/**
 * Dashboard panel showing system overview and statistics
 */
public class DashboardPanel extends JPanel {
    private JLabel totalStudentsLabel;
    private JLabel totalFacultyLabel;
    private JLabel totalCoursesLabel;
    private JLabel pendingFeesLabel;
    private JLabel waitlistLabel;
    private JLabel attendanceLabel;
    private DefaultTableModel activityModel;

    public DashboardPanel() {
        initializeComponents();
        setupLayout();
        updateStatistics();
        updateRecentActivity();
    }

    private void initializeComponents() {
        totalStudentsLabel = new JLabel("0");
        totalFacultyLabel = new JLabel("0");
        totalCoursesLabel = new JLabel("0");
        pendingFeesLabel = new JLabel("₹0");
        waitlistLabel = new JLabel("0");
        attendanceLabel = new JLabel("100%");

        // Style the numbers
        Font numberFont = new Font("Arial", Font.BOLD, 24);
        totalStudentsLabel.setFont(numberFont);
        totalFacultyLabel.setFont(numberFont);
        totalCoursesLabel.setFont(numberFont);
        pendingFeesLabel.setFont(numberFont);
        waitlistLabel.setFont(numberFont);
        attendanceLabel.setFont(numberFont);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Dashboard Overview");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Statistics cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 20, 20));

        // Student card
        JPanel studentCard = createStatCard("Total Students", totalStudentsLabel,
                new Color(34, 197, 94), "👥");

        // Faculty card
        JPanel facultyCard = createStatCard("Total Faculty", totalFacultyLabel,
                new Color(59, 130, 246), "👨‍🏫");

        // Courses card
        JPanel coursesCard = createStatCard("Total Courses", totalCoursesLabel,
                new Color(168, 85, 247), "📚");

        // Fees card
        JPanel feesCard = createStatCard("Pending Fees", pendingFeesLabel,
                new Color(245, 101, 101), "💰");
        JPanel waitlistCard = createStatCard("Waitlisted Students", waitlistLabel,
                new Color(249, 115, 22), "⏳");
        JPanel attendanceCard = createStatCard("Avg Attendance", attendanceLabel,
                new Color(16, 185, 129), "✅");

        cardsPanel.add(studentCard);
        cardsPanel.add(facultyCard);
        cardsPanel.add(coursesCard);
        cardsPanel.add(feesCard);
        cardsPanel.add(waitlistCard);
        cardsPanel.add(attendanceCard);

        // Quick actions panel
        JPanel actionsPanel = new JPanel();
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Quick Actions"));
        actionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton addStudentBtn = new JButton("Add Student");
        JButton addFacultyBtn = new JButton("Add Faculty");
        JButton addCourseBtn = new JButton("Add Course");
        JButton viewReportsBtn = new JButton("View Reports");

        // Style buttons
        Color buttonColor = new Color(37, 99, 235);
        JButton[] buttons = { addStudentBtn, addFacultyBtn, addCourseBtn, viewReportsBtn };
        for (JButton btn : buttons) {
            btn.setBackground(buttonColor);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(120, 35));
            actionsPanel.add(btn);
        }

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerLabel, BorderLayout.NORTH);
        topPanel.add(cardsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(actionsPanel, BorderLayout.CENTER);

        // Recent activity panel
        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        String[] columns = { "Timestamp", "Type", "Actor", "Details" };
        activityModel = new DefaultTableModel(columns, 0);
        JTable activityTable = new JTable(activityModel);
        activityTable.setFillsViewportHeight(true);

        recentPanel.add(new JScrollPane(activityTable), BorderLayout.CENTER);
        recentPanel.setPreferredSize(new Dimension(800, 200));
        add(recentPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 30));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(107, 114, 128));

        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.SOUTH);

        // Value
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void updateStatistics() {
        new SwingWorker<DashboardStats, Void>() {
            @Override
            protected DashboardStats doInBackground() {
                Collection<Student> students = DatabaseUtil.getAllStudents();
                Collection<Faculty> faculty = DatabaseUtil.getAllFaculty();
                Collection<Course> courses = DatabaseUtil.getAllCourses();

                double pendingFees = students.stream()
                        .mapToDouble(Student::getOutstandingFees)
                        .sum();

                int waitlistedTotal = DatabaseUtil.getAllSections().stream()
                        .mapToInt(section -> section.getWaitlistedStudentIds().size())
                        .sum();

                double avgAttendance = DatabaseUtil.getAllSections().stream()
                        .mapToDouble(section -> DatabaseUtil.getAverageAttendanceForSection(section.getSectionId()))
                        .average()
                        .orElse(100.0);

                return new DashboardStats(students.size(), faculty.size(), courses.size(),
                        pendingFees, waitlistedTotal, avgAttendance);
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();
                    totalStudentsLabel.setText(String.valueOf(stats.students()));
                    totalFacultyLabel.setText(String.valueOf(stats.faculty()));
                    totalCoursesLabel.setText(String.valueOf(stats.courses()));
                    pendingFeesLabel.setText("₹" + String.format("%.0f", stats.fees()));
                    waitlistLabel.setText(String.valueOf(stats.waitlist()));
                    attendanceLabel.setText(String.format("%.0f%%", stats.attendance()));
                } catch (Exception ex) {
                    totalStudentsLabel.setText("Error");
                }
            }
        }.execute();
    }

    private record DashboardStats(int students, int faculty, int courses, double fees, int waitlist,
            double attendance) {
    }

    public void refreshData() {
        updateStatistics();
        updateRecentActivity();
    }

    private void updateRecentActivity() {
        new SwingWorker<java.util.List<main.java.utils.AuditLogService.AuditEvent>, Void>() {
            @Override
            protected java.util.List<main.java.utils.AuditLogService.AuditEvent> doInBackground() {
                // Fetch only top 20 for dashboard
                return main.java.utils.AuditLogService.recentEvents().stream().limit(20)
                        .collect(java.util.stream.Collectors.toList());
            }

            @Override
            protected void done() {
                try {
                    java.util.List<main.java.utils.AuditLogService.AuditEvent> events = get();
                    activityModel.setRowCount(0);
                    for (main.java.utils.AuditLogService.AuditEvent event : events) {
                        activityModel.addRow(new Object[] {
                                event.getTimestamp()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                event.getType(),
                                event.getActor(),
                                event.getDetails()
                        });
                    }
                } catch (Exception ex) {
                    // Ignore or log
                }
            }
        }.execute();
    }
}

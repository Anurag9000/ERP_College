package main.java.gui.panels;

import main.java.utils.DatabaseUtil;
import main.java.models.*;
import javax.swing.*;
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
    
    public DashboardPanel() {
        initializeComponents();
        setupLayout();
        updateStatistics();
    }
    
    private void initializeComponents() {
        totalStudentsLabel = new JLabel("0");
        totalFacultyLabel = new JLabel("0");
        totalCoursesLabel = new JLabel("0");
        pendingFeesLabel = new JLabel("₹0");
        
        // Style the numbers
        Font numberFont = new Font("Arial", Font.BOLD, 24);
        totalStudentsLabel.setFont(numberFont);
        totalFacultyLabel.setFont(numberFont);
        totalCoursesLabel.setFont(numberFont);
        pendingFeesLabel.setFont(numberFont);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel headerLabel = new JLabel("Dashboard Overview");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        // Statistics cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        
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
        
        cardsPanel.add(studentCard);
        cardsPanel.add(facultyCard);
        cardsPanel.add(coursesCard);
        cardsPanel.add(feesCard);
        
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
        JButton[] buttons = {addStudentBtn, addFacultyBtn, addCourseBtn, viewReportsBtn};
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
        
        // Recent activity panel (placeholder)
        JPanel recentPanel = new JPanel();
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));
        recentPanel.add(new JLabel("No recent activity"));
        add(recentPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createStatCard(String title, JLabel valueLabel, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
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
        Collection<Student> students = DatabaseUtil.getAllStudents();
        Collection<Faculty> faculty = DatabaseUtil.getAllFaculty();
        Collection<Course> courses = DatabaseUtil.getAllCourses();
        
        totalStudentsLabel.setText(String.valueOf(students.size()));
        totalFacultyLabel.setText(String.valueOf(faculty.size()));
        totalCoursesLabel.setText(String.valueOf(courses.size()));
        
        // Calculate pending fees
        double pendingFees = students.stream()
                .mapToDouble(Student::getOutstandingFees)
                .sum();
        pendingFeesLabel.setText("₹" + String.format("%.0f", pendingFees));
    }
    
    public void refreshData() {
        updateStatistics();
    }
}
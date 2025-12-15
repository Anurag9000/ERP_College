package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Enhanced grades panel with SGPA/CGPA tracking
 */
public class GradesTrackingPanel extends JPanel {

    private final User currentUser;
    private DefaultTableModel gradesModel;

    public GradesTrackingPanel(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Academic Performance");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        // GPA Summary
        mainPanel.add(createGPASummary(), BorderLayout.NORTH);

        // Tabs for different views
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(PastelTheme.BODY_FONT);

        tabs.addTab("Current Semester", createCurrentSemesterPanel());
        tabs.addTab("All Semesters", createAllSemestersPanel());
        tabs.addTab("Course-wise Breakdown", createCourseBreakdownPanel());

        mainPanel.add(tabs, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createGPASummary() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);

        panel.add(createStatCard("CGPA", "8.45", PastelTheme.PASTEL_BLUE_DARK));
        panel.add(createStatCard("Current SGPA", "8.72", PastelTheme.PASTEL_GREEN_DARK));
        panel.add(createStatCard("Credits Completed", "84", PastelTheme.PASTEL_PURPLE_DARK));
        panel.add(createStatCard("Academic Standing", "Good", PastelTheme.PASTEL_YELLOW_DARK));

        return panel;
    }

    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(color);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(PastelTheme.BODY_FONT);
        lblLabel.setForeground(PastelTheme.TEXT_SECONDARY);

        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCurrentSemesterPanel() {
        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Course", "Credits", "Midterm", "Assignments", "Final", "Grade", "Points" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        // Mock data
        model.addRow(new Object[] { "CS101 - Data Structures", 4, "85", "90", "88", "A", "9.0" });
        model.addRow(new Object[] { "CS102 - Algorithms", 4, "78", "82", "80", "B+", "8.0" });
        model.addRow(new Object[] { "CS103 - Database Systems", 3, "92", "88", "90", "A+", "10.0" });
        model.addRow(new Object[] { "MATH201 - Linear Algebra", 3, "75", "78", "76", "B", "7.0" });
        model.addRow(new Object[] { "ENG101 - Technical Writing", 2, "88", "85", "87", "A", "9.0" });

        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel sgpaLabel = new JLabel("Semester GPA: 8.72");
        sgpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sgpaLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(sgpaLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createAllSemestersPanel() {
        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Semester", "Credits", "SGPA", "CGPA" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        // Mock data
        model.addRow(new Object[] { "Fall 2023", 20, "8.5", "8.5" });
        model.addRow(new Object[] { "Spring 2024", 22, "8.3", "8.4" });
        model.addRow(new Object[] { "Fall 2024", 20, "8.6", "8.45" });
        model.addRow(new Object[] { "Spring 2025 (Current)", 16, "8.72", "8.52" });

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel createCourseBreakdownPanel() {
        JCard card = new JCard(new BorderLayout());

        JLabel infoLabel = new JLabel("Detailed course-wise performance breakdown");
        infoLabel.setFont(PastelTheme.BODY_FONT);
        infoLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        infoLabel.setBorder(new EmptyBorder(20, 20, 20, 20));

        card.add(infoLabel, BorderLayout.NORTH);

        // TODO: Add detailed breakdown visualization
        JTextArea detailsArea = new JTextArea();
        detailsArea.setFont(PastelTheme.BODY_FONT);
        detailsArea.setEditable(false);
        detailsArea.setText("Course Performance Analysis:\n\n" +
                "Strongest Areas:\n" +
                "- Database Systems (A+ average)\n" +
                "- Data Structures (A average)\n\n" +
                "Areas for Improvement:\n" +
                "- Linear Algebra (B average)\n\n" +
                "Overall Trend: Improving ↗");

        card.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        return card;
    }
}

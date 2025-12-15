package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Student attendance view with risk alerts
 */
public class StudentAttendancePanel extends JPanel {

    private final User currentUser;
    private DefaultTableModel attendanceModel;

    public StudentAttendancePanel(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("My Attendance");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        // Summary cards
        mainPanel.add(createSummaryPanel(), BorderLayout.NORTH);

        // Attendance table
        JCard tableCard = new JCard(new BorderLayout());

        String[] columns = { "Course", "Total Classes", "Present", "Absent", "Attendance %", "Status" };
        attendanceModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(attendanceModel);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        // Mock data
        loadAttendanceData();

        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(tableCard, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 15, 0));
        panel.setOpaque(false);

        panel.add(createStatCard("Overall Attendance", "82.5%", PastelTheme.PASTEL_GREEN_DARK));
        panel.add(createStatCard("At Risk Courses", "1", PastelTheme.PASTEL_RED_DARK));
        panel.add(createStatCard("Perfect Attendance", "2", PastelTheme.PASTEL_BLUE_DARK));

        return panel;
    }

    private JPanel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblValue.setForeground(color);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(PastelTheme.BODY_FONT);
        lblLabel.setForeground(PastelTheme.TEXT_SECONDARY);

        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblLabel, BorderLayout.SOUTH);

        return card;
    }

    private void loadAttendanceData() {
        // Mock data - in production, fetch from database
        Object[][] mockData = {
                { "CS101 - Data Structures", 40, 38, 2, "95.0%", "✓ Good" },
                { "CS102 - Algorithms", 38, 30, 8, "78.9%", "✓ Good" },
                { "CS103 - Database Systems", 35, 24, 11, "68.6%", "⚠ At Risk" },
                { "MATH201 - Linear Algebra", 42, 42, 0, "100.0%", "★ Perfect" },
                { "ENG101 - Technical Writing", 30, 30, 0, "100.0%", "★ Perfect" }
        };

        for (Object[] row : mockData) {
            attendanceModel.addRow(row);
        }
    }
}

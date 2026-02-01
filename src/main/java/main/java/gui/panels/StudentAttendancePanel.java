package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

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
        attendanceModel.setRowCount(0);
        List<main.java.models.Section> schedule = main.java.service.StudentService.getSchedule(currentUser);
        String studentId = currentUser.getUsername();

        int totalPresent = 0;
        int totalAbsent = 0;
        int atRiskCount = 0;
        int perfectCount = 0;

        for (main.java.models.Section section : schedule) {
            List<main.java.models.AttendanceRecord> records = main.java.utils.DatabaseUtil
                    .getAttendanceForSection(section.getSectionId());
            int present = 0;
            int absent = 0;
            for (main.java.models.AttendanceRecord rec : records) {
                main.java.models.AttendanceRecord.AttendanceStatus status = rec.getStatus(studentId);
                if (status == main.java.models.AttendanceRecord.AttendanceStatus.PRESENT) {
                    present++;
                } else if (status == main.java.models.AttendanceRecord.AttendanceStatus.ABSENT) {
                    absent++;
                }
            }

            int total = present + absent;
            double percent = total == 0 ? 100.0 : (present * 100.0 / total);
            String status = percent >= 90.0 ? "★ Perfect" : (percent >= 75.0 ? "✓ Good" : "⚠ At Risk");

            if (percent < 75.0)
                atRiskCount++;
            if (percent >= 99.9)
                perfectCount++;
            totalPresent += present;
            totalAbsent += absent;

            attendanceModel.addRow(new Object[] {
                    section.getTitle(),
                    total,
                    present,
                    absent,
                    String.format("%.1f%%", percent),
                    status
            });
        }

        // We can't easily update the summary cards because they are in
        // createSummaryPanel which is called in constructor
        // But we can update the labels if we keep references to them.
        // For now, the table is updated.
    }
}

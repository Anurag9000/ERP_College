package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.User;
import main.java.service.StudentService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

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

        String sgpa = String.format("%.2f", StudentService.calculateSGPA(currentUser.getUsername(), null));
        String cgpa = String.format("%.2f", StudentService.calculateCGPA(currentUser.getUsername()));
        String credits = String.valueOf(StudentService.getTotalCredits(currentUser.getUsername()));
        double dSgpa = Double.parseDouble(sgpa);
        String standing = dSgpa >= 8.0 ? "Excellent" : (dSgpa >= 6.0 ? "Good" : "Probation");

        panel.add(createStatCard("CGPA", cgpa, PastelTheme.PASTEL_BLUE_DARK));
        panel.add(createStatCard("Current SGPA", sgpa, PastelTheme.PASTEL_GREEN_DARK));
        panel.add(createStatCard("Credits Completed", credits, PastelTheme.PASTEL_PURPLE_DARK));
        panel.add(createStatCard("Academic Standing", standing, PastelTheme.PASTEL_YELLOW_DARK));

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

        String[] columns = { "Course", "Credits", "Final Score", "Grade", "Points" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(currentUser.getUsername());
        for (EnrollmentRecord rec : enrollments) {
            if (rec.getStatus() != EnrollmentRecord.Status.ENROLLED)
                continue;
            Section s = DatabaseUtil.getSection(rec.getSectionId());
            if (s == null)
                continue;

            double score = rec.getFinalGrade();
            if (score <= 0 && !rec.getComponentScores().isEmpty()) {
                score = s.computeFinalScore(rec.getComponentScores());
            }

            model.addRow(new Object[] {
                    s.getTitle(),
                    DatabaseUtil.getCourseCreditHours(s.getCourseId()),
                    String.format("%.2f", score),
                    StudentService.calculateLetterGrade(score),
                    String.format("%.1f", StudentService.calculatePoints(score))
            });
        }

        card.add(new JScrollPane(table), BorderLayout.CENTER);

        double gpa = StudentService.calculateSGPA(currentUser.getUsername(), null);
        JLabel sgpaLabel = new JLabel(String.format("Semester GPA: %.2f", gpa));
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

        JLabel infoLabel = new JLabel("Detailed course-wise performance analysis");
        infoLabel.setFont(PastelTheme.BODY_FONT);
        infoLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        infoLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(infoLabel, BorderLayout.NORTH);

        StringBuilder analysis = new StringBuilder("Course Performance Analysis:\n\n");
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(currentUser.getUsername());

        List<EnrollmentRecord> graded = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .filter(e -> e.getFinalGrade() > 0 || !e.getComponentScores().isEmpty())
                .collect(Collectors.toList());

        if (graded.isEmpty()) {
            analysis.append("No graded courses available for analysis.");
        } else {
            analysis.append("Strongest Areas:\n");
            graded.stream()
                    .filter(e -> {
                        Section s = DatabaseUtil.getSection(e.getSectionId());
                        double score = e.getFinalGrade() <= 0 ? s.computeFinalScore(e.getComponentScores())
                                : e.getFinalGrade();
                        return score >= 85;
                    })
                    .forEach(e -> analysis.append("- ").append(DatabaseUtil.getSection(e.getSectionId()).getTitle())
                            .append("\n"));

            analysis.append("\nAreas for Improvement:\n");
            graded.stream()
                    .filter(e -> {
                        Section s = DatabaseUtil.getSection(e.getSectionId());
                        double score = e.getFinalGrade() <= 0 ? s.computeFinalScore(e.getComponentScores())
                                : e.getFinalGrade();
                        return score < 75;
                    })
                    .forEach(e -> analysis.append("- ").append(DatabaseUtil.getSection(e.getSectionId()).getTitle())
                            .append("\n"));
        }

        JTextArea detailsArea = new JTextArea();
        detailsArea.setFont(PastelTheme.BODY_FONT);
        detailsArea.setEditable(false);
        detailsArea.setText(analysis.toString());
        detailsArea.setMargin(new Insets(10, 10, 10, 10));

        card.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        return card;
    }
}

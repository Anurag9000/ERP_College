package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.User;
import main.java.service.GradebookService;
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
    private final JPanel contentPanel;
    private final JLabel loadingLabel;

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

        // Content placeholder
        contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(PastelTheme.PASTEL_BG);

        loadingLabel = new JLabel("Loading academic data...", SwingConstants.CENTER);
        loadingLabel.setFont(PastelTheme.BODY_FONT);
        contentPanel.add(loadingLabel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);

        // Start async load
        initData();
    }

    private void initData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<GradesData, Void>() {
            @Override
            protected GradesData doInBackground() {
                String username = currentUser.getUsername();
                double sgpa = StudentService.calculateSGPA(username, null);
                double cgpa = StudentService.calculateCGPA(username);
                int credits = StudentService.getTotalCredits(username);
                List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(username);
                List<main.java.utils.DatabaseUtil.TermGpa> history = main.java.utils.DatabaseUtil
                        .getStudentGpaHistory(username);

                return new GradesData(sgpa, cgpa, credits, enrollments, history);
            }

            @Override
            protected void done() {
                try {
                    GradesData data = get();
                    buildUI(data);
                } catch (Exception e) {
                    loadingLabel.setText("Error loading data: " + e.getMessage());
                    loadingLabel.setForeground(Color.RED);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void buildUI(GradesData data) {
        contentPanel.removeAll();

        // GPA Summary
        contentPanel.add(createGPASummary(data), BorderLayout.NORTH);

        // Tabs for different views
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(PastelTheme.BODY_FONT);

        tabs.addTab("Current Semester", createCurrentSemesterPanel(data));
        tabs.addTab("All Semesters", createAllSemestersPanel(data));
        tabs.addTab("Course-wise Breakdown", createCourseBreakdownPanel(data));

        contentPanel.add(tabs, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createGPASummary(GradesData data) {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);

        String sgpa = String.format("%.2f", data.sgpa);
        String cgpa = String.format("%.2f", data.cgpa);
        String credits = String.valueOf(data.credits);
        double dSgpa = data.sgpa;
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

    private JPanel createCurrentSemesterPanel(GradesData data) {
        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Course", "Credits", "Final Score", "Grade", "Points" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        for (EnrollmentRecord rec : data.enrollments) {
            if (rec.getStatus() != EnrollmentRecord.Status.ENROLLED)
                continue;
            Section s = DatabaseUtil.getSection(rec.getSectionId());
            if (s == null)
                continue;

            double score = rec.getFinalGrade();
            if (score <= 0 && !rec.getComponentScores().isEmpty()) {
                score = s.computeFinalScore(rec.getComponentScores());
            }

            double points = GradebookService.calculateRelativePoints(score, s.getSectionId());
            model.addRow(new Object[] {
                    s.getTitle(),
                    DatabaseUtil.getCourseCreditHours(s.getCourseId()),
                    String.format("%.2f", score),
                    StudentService.calculateLetterGrade(score), // Future: make relative too
                    String.format("%.1f", points)
            });
        }

        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel sgpaLabel = new JLabel(String.format("Semester GPA: %.2f", data.sgpa));
        sgpaLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sgpaLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(sgpaLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createAllSemestersPanel(GradesData data) {
        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Semester", "Credits", "SGPA", "CGPA" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        List<main.java.utils.DatabaseUtil.TermGpa> history = data.history;
        for (main.java.utils.DatabaseUtil.TermGpa term : history) {
            model.addRow(new Object[] {
                    term.termLabel(),
                    "-",
                    String.format("%.2f", term.gpa()),
                    String.format("%.2f", data.cgpa) // Approximate for now, ideally strictly historical
            });
        }

        if (history.isEmpty()) {
            model.addRow(new Object[] { "No history available", 0, "0.00", "0.00" });
        }

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel createCourseBreakdownPanel(GradesData data) {
        JCard card = new JCard(new BorderLayout());

        JLabel infoLabel = new JLabel("Detailed course-wise performance analysis");
        infoLabel.setFont(PastelTheme.BODY_FONT);
        infoLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        infoLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.add(infoLabel, BorderLayout.NORTH);

        StringBuilder analysis = new StringBuilder("Course Performance Analysis:\n\n");

        List<EnrollmentRecord> graded = data.enrollments.stream()
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

    // Data record to hold async results
    private record GradesData(double sgpa, double cgpa, int credits,
            List<EnrollmentRecord> enrollments,
            List<main.java.utils.DatabaseUtil.TermGpa> history) {
    }
}

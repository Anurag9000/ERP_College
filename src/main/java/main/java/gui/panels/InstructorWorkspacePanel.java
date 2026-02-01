package main.java.gui.panels;

import main.java.data.dao.AssessmentTemplateDao;
import main.java.gui.dialogs.ChangePasswordDialog;
import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.User;
import main.java.service.GradebookService;
import main.java.service.InstructorService;
import main.java.utils.DatabaseUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.time.format.DateTimeFormatter;

/**
 * Instructor operations for grade entry and section oversight.
 */
public class InstructorWorkspacePanel extends JPanel {
    private final User instructor;
    private final JComboBox<String> sectionCombo;
    private final DefaultTableModel rosterModel;
    private final JTable rosterTable;
    private final GradeAnalyticsPanel gradeAnalyticsPanel;
    private final AttendanceOverviewPanel attendanceOverviewPanel;
    private java.util.List<Section> assignedSections;

    private final JButton defineAssessmentsButton;
    private final JButton recordScoreButton;
    private final JButton computeFinalButton;
    private final JButton statsButton;
    private final JButton exportCsvButton;
    private final JButton importCsvButton;
    private final JButton manageTemplatesButton;
    private final JButton feedbackButton;
    private final JButton changePasswordButton;
    private final JButton advanceStateButton;
    private final JComboBox<Section.GradebookState> gradebookStateCombo;
    private boolean suppressGradebookStateEvents;

    public InstructorWorkspacePanel(User instructor) {
        this.instructor = instructor;
        this.sectionCombo = new JComboBox<>();
        this.rosterModel = new DefaultTableModel(new Object[] {
                "Student ID", "Status", "Final Grade"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.rosterTable = new JTable(rosterModel);
        rosterTable.setRowHeight(22);
        this.gradeAnalyticsPanel = new GradeAnalyticsPanel();
        this.attendanceOverviewPanel = new AttendanceOverviewPanel();

        defineAssessmentsButton = new JButton("Define Assessments");
        recordScoreButton = new JButton("Record Score");
        computeFinalButton = new JButton("Compute Final Grade");
        statsButton = new JButton("Class Stats");
        exportCsvButton = new JButton("Export Grades CSV");
        importCsvButton = new JButton("Import Grades CSV");
        manageTemplatesButton = new JButton("Templates");
        feedbackButton = new JButton("Rubric / Feedback");
        feedbackButton.setEnabled(false);
        advanceStateButton = new JButton("Advance State");
        gradebookStateCombo = new JComboBox<>(Section.GradebookState.values());
        changePasswordButton = new JButton("Change Password");
        changePasswordButton.setBackground(new Color(37, 99, 235).darker());
        changePasswordButton.setForeground(Color.WHITE);
        changePasswordButton.setFocusPainted(false);
        changePasswordButton.setBorderPainted(false);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(new JLabel("Section:"));
        row1.add(sectionCombo);
        row1.add(new JLabel("Gradebook State:"));
        row1.add(gradebookStateCombo);
        row1.add(advanceStateButton);
        row1.add(manageTemplatesButton);
        row1.add(changePasswordButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.add(defineAssessmentsButton);
        row2.add(recordScoreButton);
        row2.add(feedbackButton);
        row2.add(computeFinalButton);
        row2.add(statsButton);
        row2.add(exportCsvButton);
        row2.add(importCsvButton);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(row1);
        header.add(row2);

        JScrollPane rosterScroll = new JScrollPane(rosterTable);
        rosterScroll.setBorder(BorderFactory.createTitledBorder("Roster & Grades"));

        JPanel analyticsContainer = new JPanel();
        analyticsContainer.setLayout(new BoxLayout(analyticsContainer, BoxLayout.Y_AXIS));
        analyticsContainer.add(gradeAnalyticsPanel);
        analyticsContainer.add(Box.createVerticalStrut(12));
        analyticsContainer.add(attendanceOverviewPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rosterScroll, analyticsContainer);
        splitPane.setResizeWeight(0.65);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        hookListeners();
        refreshSections();
        updateMaintenanceState();
    }

    private void hookListeners() {
        sectionCombo.addActionListener(e -> refreshRoster());
        defineAssessmentsButton.addActionListener(e -> defineAssessments());
        recordScoreButton.addActionListener(e -> recordScore());
        computeFinalButton.addActionListener(e -> computeFinal());
        statsButton.addActionListener(e -> showStats());
        exportCsvButton.addActionListener(e -> exportGradesCsv());
        importCsvButton.addActionListener(e -> importGradesCsv());
        manageTemplatesButton.addActionListener(e -> openTemplateManager());
        feedbackButton.addActionListener(e -> openFeedbackDialog());
        gradebookStateCombo.addActionListener(e -> handleGradebookStateChange());
        advanceStateButton.addActionListener(e -> advanceGradebookState());
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
        rosterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateFeedbackButtonState();
            }
        });
    }

    public void updateMaintenanceState() {
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        defineAssessmentsButton.setEnabled(!maintenance);
        recordScoreButton.setEnabled(!maintenance);
        computeFinalButton.setEnabled(!maintenance);
        exportCsvButton.setEnabled(!maintenance);
        importCsvButton.setEnabled(!maintenance);
        manageTemplatesButton.setEnabled(!maintenance);
        feedbackButton.setEnabled(!maintenance && rosterTable.getSelectedRow() != -1);
        gradebookStateCombo.setEnabled(!maintenance);
        advanceStateButton.setEnabled(!maintenance);
    }

    private void refreshSections() {
        assignedSections = InstructorService.getAssignedSections(instructor);
        sectionCombo.removeAllItems();
        for (Section section : assignedSections) {
            sectionCombo.addItem(section.getSectionId() + " - " + section.getTitle());
        }
        if (sectionCombo.getItemCount() > 0) {
            sectionCombo.setSelectedIndex(0);
        }
        refreshRoster();
    }

    private Section getSelectedSection() {
        int index = sectionCombo.getSelectedIndex();
        if (index < 0 || index >= assignedSections.size()) {
            return null;
        }
        return assignedSections.get(index);
    }

    private void refreshRoster() {
        rosterModel.setRowCount(0);
        Section section = getSelectedSection();
        if (section == null) {
            suppressGradebookStateEvents = true;
            gradebookStateCombo.setSelectedItem(Section.GradebookState.DRAFT);
            suppressGradebookStateEvents = false;
            refreshAnalytics(null);
            updateFeedbackButtonState();
            return;
        }
        java.util.List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForSection(section.getSectionId());
        enrollments.stream()
                .filter(rec -> rec.getStatus() != EnrollmentRecord.Status.WAITLISTED)
                .forEach(rec -> rosterModel.addRow(new Object[] {
                        rec.getStudentId(),
                        rec.getStatus(),
                        rec.getFinalGrade()
                }));
        refreshGradebookState(section);
        refreshAnalytics(section);
        updateFeedbackButtonState();
    }

    private void defineAssessments() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }
        String input = JOptionPane.showInputDialog(this,
                "Enter assessments as component:weight comma separated (e.g., Quiz:20,Midterm:30,Final:50)",
                "Define Assessments", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        try {
            Map<String, Double> weights = parseWeights(input);
            GradebookService.defineAssessments(instructor, section.getSectionId(), weights);
            JOptionPane.showMessageDialog(this, "Assessments saved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, Double> parseWeights(String input) {
        return GradebookService.parseWeights(input);
    }

    private void recordScore() {
        Section section = getSelectedSection();
        int row = rosterTable.getSelectedRow();
        if (section == null || row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return;
        }
        String studentId = (String) rosterModel.getValueAt(row, 0);
        JComboBox<String> componentField = new JComboBox<>(
                section.getAssessmentWeights().keySet().toArray(new String[0]));
        componentField.setEditable(true);
        JTextField scoreField = new JTextField();
        JTextArea feedbackArea = new JTextArea(3, 20);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Component:"));
        form.add(componentField);
        form.add(new JLabel("Score:"));
        form.add(scoreField);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(feedbackArea), BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(this, panel, "Record Score",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        Object selectedComponent = componentField.getEditor().getItem();
        if (selectedComponent == null || selectedComponent.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Component name is required.");
            return;
        }
        String component = selectedComponent.toString().trim();
        try {
            double score = Double.parseDouble(scoreField.getText().trim());
            GradebookService.recordScore(instructor, section.getSectionId(), studentId, component, score);
            if (!feedbackArea.getText().trim().isEmpty()) {
                GradebookService.saveFeedback(instructor, section.getSectionId(), studentId, component,
                        feedbackArea.getText().trim());
            }
            JOptionPane.showMessageDialog(this, "Score saved.");
            refreshRoster();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void computeFinal() {
        Section section = getSelectedSection();
        int row = rosterTable.getSelectedRow();
        if (section == null || row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return;
        }
        String studentId = (String) rosterModel.getValueAt(row, 0);
        try {
            double finalGrade = GradebookService.computeFinal(instructor, section.getSectionId(), studentId);
            JOptionPane.showMessageDialog(this, "Final grade: " + finalGrade);
            refreshRoster();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showChangePasswordDialog() {
        java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
        JFrame frame = parent instanceof JFrame ? (JFrame) parent : null;
        ChangePasswordDialog dialog = new ChangePasswordDialog(frame, instructor.getUsername());
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            JOptionPane.showMessageDialog(this, "Password updated successfully.");
        }
    }

    private void showStats() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }
        try {
            java.util.DoubleSummaryStatistics stats = GradebookService.statsForSection(instructor,
                    section.getSectionId());
            JOptionPane.showMessageDialog(this,
                    String.format("Count: %d\nAverage: %.2f\nMax: %.2f\nMin: %.2f",
                            stats.getCount(), stats.getAverage(), stats.getMax(), stats.getMin()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportGradesCsv() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }
        java.util.List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForSection(section.getSectionId());
        if (enrollments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No enrollments to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(section.getSectionId() + "_grades.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File target = chooser.getSelectedFile();
        try (FileWriter writer = new FileWriter(target);
                CSVPrinter printer = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.builder()
                                .setHeader("Student ID", "Component", "Score", "Final Grade", "Feedback")
                                .build())) {
            for (EnrollmentRecord record : enrollments) {
                Map<String, Double> scores = record.getComponentScores();
                Map<String, String> feedbackMap = record.getComponentFeedback();
                if (scores.isEmpty()) {
                    String feedback = feedbackMap.getOrDefault("Overall", "");
                    printer.printRecord(record.getStudentId(), "", "", record.getFinalGrade(), feedback);
                } else {
                    for (Map.Entry<String, Double> entry : scores.entrySet()) {
                        String feedback = feedbackMap.getOrDefault(entry.getKey(), "");
                        printer.printRecord(record.getStudentId(), entry.getKey(), entry.getValue(),
                                record.getFinalGrade(), feedback);
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Grades exported to " + target.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to export grades: " + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importGradesCsv() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File source = chooser.getSelectedFile();
        int success = 0;
        java.util.List<String> failures = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(new FileReader(source))) {
            for (CSVRecord record : parser) {
                String studentId = record.get("Student ID");
                String component = record.get("Component");
                String scoreRaw = record.get("Score");
                if (studentId == null || studentId.isBlank()
                        || component == null || component.isBlank()
                        || scoreRaw == null || scoreRaw.isBlank()) {
                    failures.add("Missing fields on row " + record.getRecordNumber());
                    continue;
                }
                try {
                    double score = Double.parseDouble(scoreRaw.trim());
                    String componentName = component.trim();
                    GradebookService.recordScore(instructor, section.getSectionId(), studentId.trim(), componentName,
                            score);
                    if (record.isMapped("Feedback")) {
                        String feedback = record.get("Feedback");
                        if (feedback != null && !feedback.trim().isEmpty()) {
                            GradebookService.saveFeedback(instructor, section.getSectionId(), studentId.trim(),
                                    componentName, feedback.trim());
                        }
                    }
                    success++;
                } catch (Exception ex) {
                    failures.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                }
            }
            refreshRoster();
            StringBuilder summary = new StringBuilder("Imported " + success + " rows.");
            if (!failures.isEmpty()) {
                summary.append("\nIssues:\n").append(String.join("\n", failures));
            }
            JOptionPane.showMessageDialog(this, summary.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to import grades: " + ex.getMessage(),
                    "Import Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openTemplateManager() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        TemplateManagerDialog dialog = new TemplateManagerDialog(section);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openFeedbackDialog() {
        Section section = getSelectedSection();
        int row = rosterTable.getSelectedRow();
        if (section == null || row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return;
        }
        String studentId = (String) rosterModel.getValueAt(row, 0);
        FeedbackDialog dialog = new FeedbackDialog(section, studentId);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private Map<String, Double> parseTemplatePayload(String payload) {
        Map<String, Double> weights = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return weights;
        }
        String[] lines = payload.split("\\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String component = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (component.isEmpty()) {
                continue;
            }
            try {
                weights.put(component, Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return weights;
    }

    private final class TemplateManagerDialog extends JDialog {
        private final Section section;
        private final DefaultListModel<AssessmentTemplateDao.AssessmentTemplate> listModel = new DefaultListModel<>();
        private final JList<AssessmentTemplateDao.AssessmentTemplate> templateList = new JList<>(listModel);
        private final JTextArea previewArea = new JTextArea(10, 30);

        TemplateManagerDialog(Section section) {
            super(SwingUtilities.getWindowAncestor(InstructorWorkspacePanel.this), "Assessment Templates",
                    ModalityType.APPLICATION_MODAL);
            this.section = section;
            buildUi();
            refreshTemplates();
        }

        private void buildUi() {
            setLayout(new BorderLayout(10, 10));
            templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            templateList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    updatePreview();
                }
            });
            previewArea.setEditable(false);
            previewArea.setLineWrap(true);
            previewArea.setWrapStyleWord(true);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            JButton saveButton = new JButton("Save Current");
            JButton applyButton = new JButton("Apply to Section");
            JButton deleteButton = new JButton("Delete");
            actions.add(saveButton);
            actions.add(applyButton);
            actions.add(deleteButton);

            saveButton.addActionListener(e -> saveCurrentTemplate());
            applyButton.addActionListener(e -> applySelectedTemplate());
            deleteButton.addActionListener(e -> deleteSelectedTemplate());

            add(new JScrollPane(templateList), BorderLayout.WEST);
            add(new JScrollPane(previewArea), BorderLayout.CENTER);
            add(actions, BorderLayout.SOUTH);
            setSize(640, 360);
        }

        private void refreshTemplates() {
            listModel.clear();
            java.util.List<AssessmentTemplateDao.AssessmentTemplate> templates = GradebookService.listTemplates(
                    instructor,
                    section.getCourseId());
            for (AssessmentTemplateDao.AssessmentTemplate template : templates) {
                listModel.addElement(template);
            }
            updatePreview();
        }

        private void updatePreview() {
            AssessmentTemplateDao.AssessmentTemplate template = templateList.getSelectedValue();
            if (template == null) {
                previewArea.setText("Select a template to view details.");
                return;
            }
            Map<String, Double> weights = parseTemplatePayload(template.weightsJson());
            StringBuilder builder = new StringBuilder("Template: ")
                    .append(template.templateName())
                    .append("\nComponents:\n");
            weights.forEach((component, weight) -> builder.append(" • ").append(component).append(" = ").append(weight)
                    .append("\n"));
            previewArea.setText(builder.toString());
            previewArea.setCaretPosition(0);
        }

        private void saveCurrentTemplate() {
            Map<String, Double> weights = new LinkedHashMap<>(section.getAssessmentWeights());
            if (weights.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Define assessments for this section before saving a template.");
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Template name:");
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            try {
                GradebookService.saveTemplate(instructor, section.getCourseId(), name.trim(), weights);
                refreshTemplates();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to save template",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        private void applySelectedTemplate() {
            AssessmentTemplateDao.AssessmentTemplate template = templateList.getSelectedValue();
            if (template == null) {
                JOptionPane.showMessageDialog(this, "Select a template first.");
                return;
            }
            try {
                GradebookService.applyTemplate(instructor, template.id(), section.getSectionId());
                JOptionPane.showMessageDialog(this, "Template applied to section.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to apply template",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteSelectedTemplate() {
            AssessmentTemplateDao.AssessmentTemplate template = templateList.getSelectedValue();
            if (template == null) {
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete template \"" + template.templateName() + "\"?",
                    "Delete Template", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                GradebookService.deleteTemplate(instructor, template.id());
                refreshTemplates();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to delete template",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private final class FeedbackDialog extends JDialog {
        private final Section section;
        private final String studentId;
        private final Map<String, JTextArea> editors = new LinkedHashMap<>();

        FeedbackDialog(Section section, String studentId) {
            super(SwingUtilities.getWindowAncestor(InstructorWorkspacePanel.this), "Feedback for " + studentId,
                    ModalityType.APPLICATION_MODAL);
            this.section = section;
            this.studentId = studentId;
            buildUi();
        }

        private void buildUi() {
            setLayout(new BorderLayout(10, 10));
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

            Set<String> components = new LinkedHashSet<>(section.getAssessmentWeights().keySet());
            EnrollmentRecord enrollment = DatabaseUtil.getEnrollmentsForSection(section.getSectionId()).stream()
                    .filter(rec -> rec.getStudentId().equals(studentId))
                    .findFirst()
                    .orElse(null);
            if (enrollment != null) {
                components.addAll(enrollment.getComponentScores().keySet());
            }
            if (components.isEmpty()) {
                components.add("Overall");
            }

            Map<String, String> existingFeedback = GradebookService.getFeedback(instructor, section.getSectionId(),
                    studentId);
            for (String component : components) {
                JLabel label = new JLabel(component);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                JTextArea area = new JTextArea(3, 30);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setText(existingFeedback.getOrDefault(component, ""));
                editors.put(component, area);

                JPanel entry = new JPanel();
                entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
                entry.setAlignmentX(Component.LEFT_ALIGNMENT);
                entry.add(label);
                entry.add(Box.createVerticalStrut(4));
                entry.add(new JScrollPane(area));
                entry.add(Box.createVerticalStrut(10));
                listPanel.add(entry);
            }

            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(e -> saveFeedback());
            add(new JScrollPane(listPanel), BorderLayout.CENTER);
            add(saveButton, BorderLayout.SOUTH);
            pack();
        }

        private void saveFeedback() {
            Map<String, String> payload = new LinkedHashMap<>();
            editors.forEach((component, area) -> {
                String text = area.getText().trim();
                if (!text.isEmpty()) {
                    payload.put(component, text);
                }
            });
            try {
                GradebookService.saveFeedback(instructor, section.getSectionId(), studentId, payload);
                JOptionPane.showMessageDialog(this, "Feedback saved.");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to save feedback",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private final class GradeAnalyticsPanel extends JPanel {
        private final JLabel averageLabel = createMetricValue();
        private final JLabel passLabel = createMetricValue();
        private final JLabel failLabel = createMetricValue();
        private final GradeDistributionChart chart = new GradeDistributionChart();

        GradeAnalyticsPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createTitledBorder("Grade Analytics"));
            JPanel metrics = new JPanel(new GridLayout(1, 3, 8, 8));
            metrics.add(buildMetric("Average", averageLabel));
            metrics.add(buildMetric("Pass", passLabel));
            metrics.add(buildMetric("Fail", failLabel));
            add(metrics, BorderLayout.NORTH);
            add(chart, BorderLayout.CENTER);
        }

        void setData(GradebookService.GradeAnalytics analytics) {
            if (analytics == null) {
                averageLabel.setText("—");
                passLabel.setText("—");
                failLabel.setText("—");
                chart.setData(Map.of());
                return;
            }
            averageLabel.setText(String.format(Locale.ENGLISH, "%.1f", analytics.average()));
            passLabel.setText(Long.toString(analytics.passCount()));
            failLabel.setText(Long.toString(analytics.failCount()));
            chart.setData(analytics.buckets());
        }

        private JLabel createMetricValue() {
            JLabel label = new JLabel("—");
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setForeground(new Color(30, 41, 59));
            return label;
        }

        private JPanel buildMetric(String title, JLabel value) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel heading = new JLabel(title.toUpperCase(Locale.ENGLISH));
            heading.setFont(new Font("Arial", Font.BOLD, 11));
            heading.setForeground(new Color(100, 116, 139));
            panel.add(heading, BorderLayout.NORTH);
            panel.add(value, BorderLayout.CENTER);
            return panel;
        }
    }

    private final class GradeDistributionChart extends JPanel {
        private Map<String, Long> data = Map.of();

        GradeDistributionChart() {
            setPreferredSize(new Dimension(260, 160));
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        void setData(Map<String, Long> data) {
            this.data = data == null ? Map.of() : data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth() - 40;
            int height = getHeight() - 40;
            int x = 20;
            int y = 10;
            if (data.isEmpty()) {
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("No grade data yet.", x, y + height / 2);
                g2.dispose();
                return;
            }
            long max = data.values().stream().mapToLong(Long::longValue).max().orElse(1);
            int barWidth = Math.max(20, width / data.size() - 10);
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                double ratio = entry.getValue() / (double) max;
                int barHeight = (int) (height * ratio);
                g2.setColor(new Color(59, 130, 246));
                g2.fillRoundRect(x, y + height - barHeight, barWidth, barHeight, 8, 8);
                g2.setColor(new Color(71, 85, 105));
                g2.setFont(g2.getFont().deriveFont(10f));
                g2.drawString(entry.getKey(), x, y + height + 12);
                g2.drawString(String.valueOf(entry.getValue()), x, y + height - barHeight - 4);
                x += barWidth + 12;
            }
            g2.dispose();
        }
    }

    private final class AttendanceOverviewPanel extends JPanel {
        private final JLabel averageLabel = createMetricValue();
        private final JLabel sessionsLabel = createMetricValue();
        private final AttendanceChart chart = new AttendanceChart();

        AttendanceOverviewPanel() {
            setLayout(new BorderLayout(8, 8));
            setBorder(BorderFactory.createTitledBorder("Attendance Trends"));
            JPanel metrics = new JPanel(new GridLayout(1, 2, 8, 8));
            metrics.add(buildMetric("Average Attendance", averageLabel));
            metrics.add(buildMetric("Sessions", sessionsLabel));
            add(metrics, BorderLayout.NORTH);
            add(chart, BorderLayout.CENTER);
        }

        void setData(GradebookService.AttendanceAnalytics analytics) {
            if (analytics == null) {
                averageLabel.setText("—");
                sessionsLabel.setText("0");
                chart.setData(java.util.List.of());
                return;
            }
            averageLabel.setText(String.format(Locale.ENGLISH, "%.1f%%", analytics.averagePercent()));
            sessionsLabel.setText(Long.toString(analytics.sessions()));
            chart.setData(analytics.snapshots());
        }

        private JLabel createMetricValue() {
            JLabel label = new JLabel("—");
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setForeground(new Color(30, 41, 59));
            return label;
        }

        private JPanel buildMetric(String title, JLabel value) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel heading = new JLabel(title.toUpperCase(Locale.ENGLISH));
            heading.setFont(new Font("Arial", Font.BOLD, 11));
            heading.setForeground(new Color(100, 116, 139));
            panel.add(heading, BorderLayout.NORTH);
            panel.add(value, BorderLayout.CENTER);
            return panel;
        }
    }

    private final class AttendanceChart extends JPanel {
        private java.util.List<GradebookService.AttendanceSnapshot> snapshots = java.util.List.of();
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        AttendanceChart() {
            setPreferredSize(new Dimension(260, 160));
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        void setData(java.util.List<GradebookService.AttendanceSnapshot> snapshots) {
            this.snapshots = snapshots == null ? java.util.List.of() : snapshots;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (snapshots.isEmpty()) {
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("No attendance sessions recorded.", 10, getHeight() / 2);
                g2.dispose();
                return;
            }
            int padding = 20;
            int width = getWidth() - padding * 2;
            int height = getHeight() - padding * 2;
            int xStep = snapshots.size() <= 1 ? width : width / (snapshots.size() - 1);
            int prevX = padding;
            int prevY = padding + height - (int) (height * (snapshots.get(0).percentage() / 100.0));
            g2.setColor(new Color(59, 130, 246));
            g2.setStroke(new BasicStroke(2f));
            for (int i = 1; i < snapshots.size(); i++) {
                GradebookService.AttendanceSnapshot snapshot = snapshots.get(i);
                int x = padding + (i * xStep);
                int y = padding + height - (int) (height * (snapshot.percentage() / 100.0));
                g2.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(g2.getFont().deriveFont(10f));
            for (int i = 0; i < snapshots.size(); i++) {
                GradebookService.AttendanceSnapshot snapshot = snapshots.get(i);
                int x = padding + (i * xStep);
                int y = padding + height - (int) (height * (snapshot.percentage() / 100.0));
                g2.fillOval(x - 3, y - 3, 6, 6);
                g2.drawString(snapshot.date().format(formatter), x - 12, getHeight() - 4);
            }
            g2.dispose();
        }
    }

    private void refreshGradebookState(Section section) {
        suppressGradebookStateEvents = true;
        try {
            Section.GradebookState state = GradebookService.getGradebookState(instructor, section.getSectionId());
            gradebookStateCombo.setSelectedItem(state);
        } catch (Exception ex) {
            gradebookStateCombo.setSelectedItem(Section.GradebookState.DRAFT);
        } finally {
            suppressGradebookStateEvents = false;
        }
    }

    private void handleGradebookStateChange() {
        if (suppressGradebookStateEvents) {
            return;
        }
        Section section = getSelectedSection();
        if (section == null) {
            return;
        }
        Section.GradebookState selected = (Section.GradebookState) gradebookStateCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            GradebookService.updateGradebookState(instructor, section.getSectionId(), selected);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update state", JOptionPane.ERROR_MESSAGE);
            refreshGradebookState(section);
        }
    }

    private void advanceGradebookState() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        Section.GradebookState current = (Section.GradebookState) gradebookStateCombo.getSelectedItem();
        Section.GradebookState next = nextState(current);
        try {
            GradebookService.updateGradebookState(instructor, section.getSectionId(), next);
            refreshGradebookState(section);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to advance state", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Section.GradebookState nextState(Section.GradebookState current) {
        if (current == null) {
            return Section.GradebookState.DRAFT;
        }
        return switch (current) {
            case DRAFT -> Section.GradebookState.SUBMITTED;
            case SUBMITTED -> Section.GradebookState.PUBLISHED;
            default -> Section.GradebookState.PUBLISHED;
        };
    }

    private void updateFeedbackButtonState() {
        boolean hasSelection = rosterTable.getSelectedRow() != -1;
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        feedbackButton.setEnabled(hasSelection && !maintenance);
    }

    private void refreshAnalytics(Section section) {
        if (section == null) {
            gradeAnalyticsPanel.setData(null);
            attendanceOverviewPanel.setData(null);
            return;
        }
        try {
            GradebookService.GradeAnalytics gradeAnalytics = GradebookService.gradeAnalyticsForSection(instructor,
                    section.getSectionId());
            gradeAnalyticsPanel.setData(gradeAnalytics);
        } catch (Exception ex) {
            gradeAnalyticsPanel.setData(null);
        }
        try {
            GradebookService.AttendanceAnalytics attendanceAnalytics = GradebookService
                    .attendanceAnalyticsForSection(instructor, section.getSectionId());
            attendanceOverviewPanel.setData(attendanceAnalytics);
        } catch (Exception ex) {
            attendanceOverviewPanel.setData(null);
        }
    }
}

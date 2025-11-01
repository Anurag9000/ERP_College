package main.java.gui.panels;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.User;
import main.java.service.GradebookService;
import main.java.service.InstructorService;
import main.java.utils.DatabaseUtil;
import main.java.gui.dialogs.ChangePasswordDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

/**
 * Instructor operations for grade entry and section oversight.
 */
public class InstructorWorkspacePanel extends JPanel {
    private final User instructor;
    private final JComboBox<String> sectionCombo;
    private final DefaultTableModel rosterModel;
    private final JTable rosterTable;
    private java.util.List<Section> assignedSections;

    private final JButton defineAssessmentsButton;
    private final JButton recordScoreButton;
    private final JButton computeFinalButton;
    private final JButton statsButton;
    private final JButton changePasswordButton;

    public InstructorWorkspacePanel(User instructor) {
        this.instructor = instructor;
        this.sectionCombo = new JComboBox<>();
        this.rosterModel = new DefaultTableModel(new Object[]{
                "Student ID", "Status", "Final Grade"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.rosterTable = new JTable(rosterModel);
        rosterTable.setRowHeight(22);

        defineAssessmentsButton = new JButton("Define Assessments");
        recordScoreButton = new JButton("Record Score");
        computeFinalButton = new JButton("Compute Final Grade");
        statsButton = new JButton("Class Stats");
        changePasswordButton = new JButton("Change Password");
        changePasswordButton.setBackground(new Color(37, 99, 235).darker());
        changePasswordButton.setForeground(Color.WHITE);
        changePasswordButton.setFocusPainted(false);
        changePasswordButton.setBorderPainted(false);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Section:"));
        top.add(sectionCombo);
        top.add(defineAssessmentsButton);
        top.add(recordScoreButton);
        top.add(computeFinalButton);
        top.add(statsButton);
        top.add(Box.createHorizontalStrut(20));
        top.add(changePasswordButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(rosterTable), BorderLayout.CENTER);

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
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
    }

    public void updateMaintenanceState() {
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        defineAssessmentsButton.setEnabled(!maintenance);
        recordScoreButton.setEnabled(!maintenance);
        computeFinalButton.setEnabled(!maintenance);
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
            return;
        }
        java.util.List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForSection(section.getSectionId());
        enrollments.stream()
                .filter(rec -> rec.getStatus() != EnrollmentRecord.Status.WAITLISTED)
                .forEach(rec -> rosterModel.addRow(new Object[]{
                        rec.getStudentId(),
                        rec.getStatus(),
                        rec.getFinalGrade()
                }));
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
        Map<String, Double> weights = new LinkedHashMap<>();
        for (String token : input.split(",")) {
            String[] parts = token.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
            weights.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
        }
        return weights;
    }

    private void recordScore() {
        Section section = getSelectedSection();
        int row = rosterTable.getSelectedRow();
        if (section == null || row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return;
        }
        String studentId = (String) rosterModel.getValueAt(row, 0);
        String component = JOptionPane.showInputDialog(this, "Component name:");
        if (component == null || component.trim().isEmpty()) {
            return;
        }
        String scoreInput = JOptionPane.showInputDialog(this, "Score for " + component + ":");
        if (scoreInput == null || scoreInput.trim().isEmpty()) {
            return;
        }
        try {
            double score = Double.parseDouble(scoreInput.trim());
            GradebookService.recordScore(instructor, section.getSectionId(), studentId, component.trim(), score);
            JOptionPane.showMessageDialog(this, "Score saved.");
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
            java.util.DoubleSummaryStatistics stats = GradebookService.statsForSection(instructor, section.getSectionId());
            JOptionPane.showMessageDialog(this,
                    String.format("Count: %d\nAverage: %.2f\nMax: %.2f\nMin: %.2f",
                            stats.getCount(), stats.getAverage(), stats.getMax(), stats.getMin()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

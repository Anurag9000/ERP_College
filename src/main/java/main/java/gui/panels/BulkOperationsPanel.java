package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.service.BulkImportExportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Panel for bulk import/export operations
 */
public class BulkOperationsPanel extends JPanel {

    public BulkOperationsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Bulk Import/Export");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 20, 20));
        mainPanel.setOpaque(false);

        mainPanel.add(createImportSection());
        mainPanel.add(createExportSection());

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createImportSection() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Import Data");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonsPanel.setOpaque(false);

        // Students
        JButton importStudentsBtn = new JButton("Import Students");
        PastelTheme.styleButtonPrimary(importStudentsBtn);
        importStudentsBtn.addActionListener(e -> importStudents());

        JLabel studentsDesc = new JLabel("CSV: studentId,firstName,lastName,email,phone,dob,address,course,semester");
        studentsDesc.setFont(PastelTheme.BODY_FONT);
        studentsDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        // Faculty
        JButton importFacultyBtn = new JButton("Import Faculty");
        PastelTheme.styleButtonPrimary(importFacultyBtn);
        importFacultyBtn.addActionListener(e -> importFaculty());

        JLabel facultyDesc = new JLabel(
                "CSV: facultyId,firstName,lastName,email,phone,dept,designation,qualification,salary");
        facultyDesc.setFont(PastelTheme.BODY_FONT);
        facultyDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        // Template download
        JButton downloadTemplateBtn = new JButton("Download Templates");
        PastelTheme.styleButtonSecondary(downloadTemplateBtn);
        downloadTemplateBtn.addActionListener(e -> downloadTemplates());

        JLabel templateDesc = new JLabel("Get CSV templates with correct format");
        templateDesc.setFont(PastelTheme.BODY_FONT);
        templateDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        buttonsPanel.add(importStudentsBtn);
        buttonsPanel.add(studentsDesc);
        buttonsPanel.add(importFacultyBtn);
        buttonsPanel.add(facultyDesc);
        buttonsPanel.add(downloadTemplateBtn);
        buttonsPanel.add(templateDesc);

        card.add(buttonsPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createExportSection() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Export Data");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonsPanel.setOpaque(false);

        // Students
        JButton exportStudentsBtn = new JButton("Export Students");
        PastelTheme.styleButtonPrimary(exportStudentsBtn);
        exportStudentsBtn.addActionListener(e -> exportStudents());

        JLabel studentsDesc = new JLabel("Export all student records to CSV");
        studentsDesc.setFont(PastelTheme.BODY_FONT);
        studentsDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        // Faculty
        JButton exportFacultyBtn = new JButton("Export Faculty");
        PastelTheme.styleButtonPrimary(exportFacultyBtn);
        exportFacultyBtn.addActionListener(e -> exportFaculty());

        JLabel facultyDesc = new JLabel("Export all faculty records to CSV");
        facultyDesc.setFont(PastelTheme.BODY_FONT);
        facultyDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        // Schedules
        JButton exportSchedulesBtn = new JButton("Export Schedules");
        PastelTheme.styleButtonPrimary(exportSchedulesBtn);
        exportSchedulesBtn.addActionListener(e -> exportSchedules());

        JLabel schedulesDesc = new JLabel("Export all section schedules to CSV");
        schedulesDesc.setFont(PastelTheme.BODY_FONT);
        schedulesDesc.setForeground(PastelTheme.TEXT_SECONDARY);

        buttonsPanel.add(exportStudentsBtn);
        buttonsPanel.add(studentsDesc);
        buttonsPanel.add(exportFacultyBtn);
        buttonsPanel.add(facultyDesc);
        buttonsPanel.add(exportSchedulesBtn);
        buttonsPanel.add(schedulesDesc);

        card.add(buttonsPanel, BorderLayout.CENTER);

        return card;
    }

    private void importStudents() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                List<String> errors = BulkImportExportService.importStudents(file);
                if (errors.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Students imported successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showErrors("Import Errors", errors);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error importing students: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importFaculty() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                List<String> errors = BulkImportExportService.importFaculty(file);
                if (errors.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Faculty imported successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showErrors("Import Errors", errors);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error importing faculty: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportStudents() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("students_export.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BulkImportExportService.exportStudents(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Students exported successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting students: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportFaculty() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("faculty_export.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BulkImportExportService.exportFaculty(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Faculty exported successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting faculty: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportSchedules() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("schedules_export.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BulkImportExportService.exportSchedules(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Schedules exported successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting schedules: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void downloadTemplates() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Directory to Save Templates");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            try {
                File studentFile = new File(dir, "student_template.csv");
                File facultyFile = new File(dir, "faculty_template.csv");

                BulkImportExportService.generateStudentTemplate(studentFile);
                BulkImportExportService.generateFacultyTemplate(facultyFile);

                JOptionPane.showMessageDialog(this,
                        "Templates saved to:\n" +
                                studentFile.getAbsolutePath() + "\n" +
                                facultyFile.getAbsolutePath(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving templates: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showErrors(String title, List<String> errors) {
        JTextArea textArea = new JTextArea(String.join("\n", errors));
        textArea.setEditable(false);
        textArea.setFont(PastelTheme.BODY_FONT);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.WARNING_MESSAGE);
    }
}

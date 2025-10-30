package main.java.gui.panels;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.EnrollmentService;
import main.java.service.StudentService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

/**
 * Student-facing self-service workspace for catalog, schedule, and grades.
 */
public class StudentSelfServicePanel extends JPanel {
    private final User currentUser;
    private Student studentProfile;

    private final DefaultTableModel catalogModel;
    private final DefaultTableModel scheduleModel;
    private final DefaultTableModel gradesModel;

    private final JLabel maintenanceBanner;
    private final JButton registerButton;
    private final JButton dropButton;
    private final JButton transcriptButton;

    private final JTable catalogTable;
    private final JTable scheduleTable;
    private final JTable gradesTable;

    public StudentSelfServicePanel(User currentUser) {
        this.currentUser = currentUser;
        this.catalogModel = new DefaultTableModel(new Object[]{
                "Section", "Course", "Day", "Time", "Location", "Seats", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.scheduleModel = new DefaultTableModel(new Object[]{
                "Section", "Course", "Day", "Time", "Location"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.gradesModel = new DefaultTableModel(new Object[]{
                "Section", "Component", "Score", "Weight", "Final"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.catalogTable = new JTable(catalogModel);
        this.catalogTable.setRowHeight(22);
        this.scheduleTable = new JTable(scheduleModel);
        this.scheduleTable.setRowHeight(22);
        this.gradesTable = new JTable(gradesModel);
        this.gradesTable.setRowHeight(22);

        this.registerButton = new JButton("Register");
        this.dropButton = new JButton("Drop");
        this.transcriptButton = new JButton("Download Transcript (CSV)");
        this.maintenanceBanner = new JLabel();
        maintenanceBanner.setForeground(Color.RED.darker());
        maintenanceBanner.setFont(new Font("Arial", Font.BOLD, 12));

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        hookListeners();
        refreshProfile();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Student Self Service");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(title, BorderLayout.WEST);
        panel.add(maintenanceBanner, BorderLayout.EAST);
        return panel;
    }

    private JComponent createBody() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog & Registration", buildCatalogTab());
        tabs.addTab("Timetable", new JScrollPane(scheduleTable));
        tabs.addTab("Grades", new JScrollPane(gradesTable));
        tabs.addTab("Transcript", buildTranscriptTab());
        return tabs;
    }

    private JPanel buildCatalogTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(registerButton);
        controls.add(dropButton);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTranscriptTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        transcriptButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(transcriptButton);
        panel.add(Box.createVerticalStrut(10));
        JTextArea info = new JTextArea("Downloads a CSV copy of completed course grades.");
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setBackground(getBackground());
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        return panel;
    }

    private void hookListeners() {
        registerButton.addActionListener(e -> performRegistration());
        dropButton.addActionListener(e -> performDrop());
        transcriptButton.addActionListener(e -> exportTranscript());
    }

    private void refreshProfile() {
        this.studentProfile = StudentService.getProfile(currentUser);
        updateMaintenanceState();
        populateCatalog();
        populateSchedule();
        populateGrades();
    }

    private void updateMaintenanceState() {
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        maintenanceBanner.setText(maintenance ? "System is in maintenance mode. Changes disabled." : "");
        registerButton.setEnabled(!maintenance);
        dropButton.setEnabled(!maintenance);
    }

    private void populateCatalog() {
        catalogModel.setRowCount(0);
        Map<String, EnrollmentRecord.Status> statusMap = buildStatusMap();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        for (Section section : DatabaseUtil.getAllSections()) {
            String status = statusMap.containsKey(section.getSectionId())
                    ? statusMap.get(section.getSectionId()).name()
                    : (section.isFull() ? "FULL" : "OPEN");
            catalogModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId() + " • " + section.getTitle(),
                    section.getDayOfWeek(),
                    section.getStartTime().format(timeFormatter) + "-" + section.getEndTime().format(timeFormatter),
                    section.getLocation(),
                    section.getAvailableSeats() + "/" + section.getCapacity(),
                    status
            });
        }
    }

    private void populateSchedule() {
        scheduleModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<Section> schedule = DatabaseUtil.getScheduleForStudent(studentProfile.getStudentId());
        for (Section section : schedule) {
            scheduleModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId() + " • " + section.getTitle(),
                    section.getDayOfWeek(),
                    section.getStartTime().format(formatter) + "-" + section.getEndTime().format(formatter),
                    section.getLocation()
            });
        }
    }

    private void populateGrades() {
        gradesModel.setRowCount(0);
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(studentProfile.getStudentId());
        for (EnrollmentRecord record : enrollments) {
            if (record.getStatus() != EnrollmentRecord.Status.ENROLLED && record.getStatus() != EnrollmentRecord.Status.DROPPED) {
                continue;
            }
            Section section = DatabaseUtil.getSection(record.getSectionId());
            Map<String, Double> weights = section != null ? section.getAssessmentWeights() : Collections.emptyMap();
            double finalGrade = record.getFinalGrade();
            if (record.getComponentScores().isEmpty()) {
                gradesModel.addRow(new Object[]{
                        record.getSectionId(),
                        "—",
                        "—",
                        "—",
                        finalGrade == 0 ? "Pending" : finalGrade
                });
            } else {
                for (Map.Entry<String, Double> entry : record.getComponentScores().entrySet()) {
                    String component = entry.getKey();
                    Double score = entry.getValue();
                    Double weight = weights.getOrDefault(component, 0.0);
                    gradesModel.addRow(new Object[]{
                            record.getSectionId(), component, score, weight, finalGrade
                    });
                }
            }
        }
    }

    private Map<String, EnrollmentRecord.Status> buildStatusMap() {
        Map<String, EnrollmentRecord.Status> statusMap = new HashMap<>();
        for (EnrollmentRecord record : DatabaseUtil.getEnrollmentsForStudent(studentProfile.getStudentId())) {
            statusMap.put(record.getSectionId(), record.getStatus());
        }
        return statusMap;
    }

    private void performRegistration() {
        int row = catalogTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        String sectionId = (String) catalogModel.getValueAt(row, 0);
        try {
            EnrollmentService.registerSection(currentUser, studentProfile.getStudentId(), sectionId);
            JOptionPane.showMessageDialog(this, "Registration request processed.");
            refreshProfile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to register", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performDrop() {
        int row = catalogTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        String sectionId = (String) catalogModel.getValueAt(row, 0);
        try {
            EnrollmentService.dropSection(currentUser, studentProfile.getStudentId(), sectionId);
            JOptionPane.showMessageDialog(this, "Section dropped." );
            refreshProfile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to drop", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportTranscript() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript_" + studentProfile.getStudentId() + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Section,Course,Final Grade\n");
                for (EnrollmentRecord record : DatabaseUtil.getEnrollmentsForStudent(studentProfile.getStudentId())) {
                    Section section = DatabaseUtil.getSection(record.getSectionId());
                    String courseName = section != null ? section.getCourseId() + " " + section.getTitle() : record.getSectionId();
                    writer.write(record.getSectionId() + "," + courseName + "," + record.getFinalGrade() + "\n");
                }
                JOptionPane.showMessageDialog(this, "Transcript exported to " + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to export", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

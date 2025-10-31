package main.java.gui.panels;

import main.java.models.Course;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.EnrollmentService;
import main.java.service.StudentService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Student-facing self-service workspace for catalog, schedule, and grades.
 */
public class StudentSelfServicePanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final User currentUser;
    private Student studentProfile;

    private final DefaultTableModel catalogModel;
    private final DefaultTableModel scheduleModel;
    private final DefaultTableModel gradesModel;

    private final JLabel maintenanceBanner;
    private final JButton registerButton;
    private final JButton dropButton;
    private final JButton transcriptButton;
    private final JTextField catalogSearchField;
    private final JComboBox<String> dayFilter;
    private final JCheckBox openOnlyCheck;

    private final JTable catalogTable;
    private final JTable scheduleTable;
    private final JTable gradesTable;

    private List<Section> catalogSections = new ArrayList<>();
    private Map<String, EnrollmentRecord.Status> enrollmentStatusBySection = new HashMap<>();

    public StudentSelfServicePanel(User currentUser) {
        this.currentUser = currentUser;
        this.catalogModel = new DefaultTableModel(new Object[]{
                "Section", "Course", "Day", "Time", "Location", "Seats", "Status", "Prerequisites"
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

        catalogTable = new JTable(catalogModel);
        catalogTable.setRowHeight(22);
        catalogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scheduleTable = new JTable(scheduleModel);
        scheduleTable.setRowHeight(22);
        gradesTable = new JTable(gradesModel);
        gradesTable.setRowHeight(22);

        registerButton = new JButton("Register");
        dropButton = new JButton("Drop");
        transcriptButton = new JButton("Download Transcript (CSV)");

        catalogSearchField = new JTextField(18);
        catalogSearchField.setToolTipText("Search by section, course, or instructor");
        dayFilter = new JComboBox<>(buildDayFilterModel());
        openOnlyCheck = new JCheckBox("Open seats only");
        openOnlyCheck.setOpaque(false);

        maintenanceBanner = new JLabel();
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
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(registerButton);
        controls.add(dropButton);
        controls.add(new JLabel("Search:"));
        controls.add(catalogSearchField);
        controls.add(new JLabel("Day:"));
        controls.add(dayFilter);
        controls.add(openOnlyCheck);

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
        JTextArea info = new JTextArea("""
                Downloads a CSV copy of your course grades for personal reference.
                For official transcripts, contact the registrar's office.
                """);
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setOpaque(false);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        return panel;
    }

    private String[] buildDayFilterModel() {
        DayOfWeek[] values = DayOfWeek.values();
        String[] options = new String[values.length + 1];
        options[0] = "All Days";
        for (int i = 0; i < values.length; i++) {
            options[i + 1] = capitalize(values[i].name());
        }
        return options;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String lower = value.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void hookListeners() {
        registerButton.addActionListener(e -> performRegistration());
        dropButton.addActionListener(e -> performDrop());
        transcriptButton.addActionListener(e -> exportTranscript());

        catalogTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });

        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyCatalogFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyCatalogFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyCatalogFilters();
            }
        };

        catalogSearchField.getDocument().addDocumentListener(filterListener);
        dayFilter.addActionListener(e -> applyCatalogFilters());
        openOnlyCheck.addActionListener(e -> applyCatalogFilters());
    }

    private void refreshProfile() {
        try {
            this.studentProfile = StudentService.getProfile(currentUser);
            updateMaintenanceState();
            enrollmentStatusBySection = buildStatusMap();
            populateCatalog();
            populateSchedule();
            populateGrades();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Profile not found", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMaintenanceState() {
        maintenanceBanner.setText(DatabaseUtil.isMaintenanceMode()
                ? "System is in maintenance mode. Changes disabled."
                : "");
        updateActionButtons();
    }

    public void refreshForMaintenance() {
        updateMaintenanceState();
    }

    private void populateCatalog() {
        catalogSections = new ArrayList<>(DatabaseUtil.getAllSections());
        catalogSections.sort(Comparator.comparing(Section::getCourseId).thenComparing(Section::getSectionId));
        applyCatalogFilters();
    }

    private void applyCatalogFilters() {
        catalogModel.setRowCount(0);
        if (studentProfile == null) {
            updateActionButtons();
            return;
        }

        String search = catalogSearchField.getText().trim().toLowerCase(Locale.ENGLISH);
        DayOfWeek selectedDay = resolveSelectedDay();
        boolean onlyOpen = openOnlyCheck.isSelected();

        for (Section section : catalogSections) {
            if (selectedDay != null && section.getDayOfWeek() != selectedDay) {
                continue;
            }

            Course course = DatabaseUtil.getCourse(section.getCourseId());
            Faculty faculty = DatabaseUtil.getFaculty(section.getFacultyId());
            String courseTitle = course != null ? course.getCourseName() : section.getTitle();
            String instructorName = faculty != null ? faculty.getFullName() : "TBA";
            String haystack = (section.getSectionId() + " " + section.getCourseId() + " " + courseTitle + " " + instructorName)
                    .toLowerCase(Locale.ENGLISH);
            if (!search.isEmpty() && !haystack.contains(search)) {
                continue;
            }

            EnrollmentRecord.Status status = enrollmentStatusBySection.get(section.getSectionId());
            int enrolledCount = section.getEnrolledStudentIds().size();
            int availableSeats = Math.max(0, section.getCapacity() - enrolledCount);
            boolean sectionFull = availableSeats <= 0;

            if (onlyOpen && status == null && sectionFull) {
                continue;
            }

            List<String> prereqs = DatabaseUtil.getCoursePrerequisites(section.getCourseId());
            List<String> missing = DatabaseUtil.getMissingPrerequisites(studentProfile.getStudentId(), section.getCourseId());

            String statusText;
            if (status == EnrollmentRecord.Status.ENROLLED) {
                statusText = "Enrolled";
            } else if (status == EnrollmentRecord.Status.WAITLISTED) {
                statusText = "Waitlisted";
            } else if (!missing.isEmpty()) {
                statusText = "Blocked (prereqs)";
            } else if (sectionFull) {
                statusText = "Full";
            } else {
                statusText = "Open";
            }

            String prereqText;
            if (prereqs.isEmpty()) {
                prereqText = "None";
            } else if (missing.isEmpty()) {
                prereqText = "Met: " + String.join(", ", prereqs);
            } else {
                prereqText = "Missing: " + String.join(", ", missing);
            }

            catalogModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId() + " - " + courseTitle,
                    capitalize(section.getDayOfWeek().name()),
                    section.getStartTime().format(TIME_FORMATTER) + "-" + section.getEndTime().format(TIME_FORMATTER),
                    section.getLocation(),
                    availableSeats + "/" + section.getCapacity(),
                    statusText,
                    prereqText
            });
        }

        updateActionButtons();
    }

    private DayOfWeek resolveSelectedDay() {
        int index = dayFilter.getSelectedIndex();
        if (index <= 0) {
            return null;
        }
        DayOfWeek[] values = DayOfWeek.values();
        if (index - 1 < values.length) {
            return values[index - 1];
        }
        return null;
    }

    private void populateSchedule() {
        scheduleModel.setRowCount(0);
        List<Section> schedule = DatabaseUtil.getScheduleForStudent(studentProfile.getStudentId());
        for (Section section : schedule) {
            scheduleModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId() + " - " + section.getTitle(),
                    capitalize(section.getDayOfWeek().name()),
                    section.getStartTime().format(TIME_FORMATTER) + "-" + section.getEndTime().format(TIME_FORMATTER),
                    section.getLocation()
            });
        }
    }

    private void populateGrades() {
        gradesModel.setRowCount(0);
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForStudent(studentProfile.getStudentId());
        for (EnrollmentRecord record : enrollments) {
            if (record.getStatus() != EnrollmentRecord.Status.ENROLLED
                    && record.getStatus() != EnrollmentRecord.Status.DROPPED) {
                continue;
            }
            Section section = DatabaseUtil.getSection(record.getSectionId());
            Map<String, Double> weights = section != null ? section.getAssessmentWeights() : Collections.emptyMap();
            double finalGrade = record.getFinalGrade();
            if (record.getComponentScores().isEmpty()) {
                gradesModel.addRow(new Object[]{
                        record.getSectionId(),
                        "-",
                        "-",
                        "-",
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
        if (studentProfile == null) {
            return statusMap;
        }
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
        int modelRow = catalogTable.convertRowIndexToModel(row);
        String sectionId = (String) catalogModel.getValueAt(modelRow, 0);
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
        int modelRow = catalogTable.convertRowIndexToModel(row);
        String sectionId = (String) catalogModel.getValueAt(modelRow, 0);
        try {
            EnrollmentService.dropSection(currentUser, studentProfile.getStudentId(), sectionId);
            JOptionPane.showMessageDialog(this, "Section dropped.");
            refreshProfile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to drop", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateActionButtons() {
        registerButton.setEnabled(false);
        dropButton.setEnabled(false);

        if (DatabaseUtil.isMaintenanceMode() || studentProfile == null) {
            return;
        }

        int viewRow = catalogTable.getSelectedRow();
        if (viewRow == -1) {
            return;
        }
        int modelRow = catalogTable.convertRowIndexToModel(viewRow);
        String sectionId = (String) catalogModel.getValueAt(modelRow, 0);

        EnrollmentRecord.Status status = enrollmentStatusBySection.get(sectionId);
        if (status == EnrollmentRecord.Status.ENROLLED || status == EnrollmentRecord.Status.WAITLISTED) {
            dropButton.setEnabled(true);
            return;
        }

        Section section = DatabaseUtil.getSection(sectionId);
        if (section != null) {
            List<String> missing = DatabaseUtil.getMissingPrerequisites(studentProfile.getStudentId(), section.getCourseId());
            registerButton.setEnabled(missing.isEmpty());
        } else {
            registerButton.setEnabled(true);
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


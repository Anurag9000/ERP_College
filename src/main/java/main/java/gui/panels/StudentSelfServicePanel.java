package main.java.gui.panels;

import main.java.models.Course;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.models.NotificationMessage;
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
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

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
    private final JButton exportScheduleButton;
    private final JButton reminderButton;
    private final JTextField catalogSearchField;
    private final JComboBox<String> dayFilter;
    private final JCheckBox openOnlyCheck;

    private final JTable catalogTable;
    private final JTable scheduleTable;
    private final JTable gradesTable;

    private List<Section> catalogSections = new ArrayList<>();
    private Map<String, EnrollmentRecord.Status> enrollmentStatusBySection = new HashMap<>();
    private final JLabel gpaLabel;
    private final JLabel creditsLabel;
    private final JLabel standingLabel;
    private final JLabel standingAlertLabel;
    private final JLabel financeTotalLabel;
    private final JLabel financePaidLabel;
    private final JLabel financeOutstandingLabel;
    private final JLabel financeNextDueLabel;
    private final JLabel catalogAdvisoryLabel;

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
        exportScheduleButton = new JButton("Export Timetable (.ics)");
        reminderButton = new JButton("Send Payment Reminder");

        catalogSearchField = new JTextField(18);
        catalogSearchField.setToolTipText("Search by section, course, or instructor");
        dayFilter = new JComboBox<>(buildDayFilterModel());
        openOnlyCheck = new JCheckBox("Open seats only");
        openOnlyCheck.setOpaque(false);

        maintenanceBanner = new JLabel();
        maintenanceBanner.setForeground(Color.RED.darker());
        maintenanceBanner.setFont(new Font("Arial", Font.BOLD, 12));

        gpaLabel = createSummaryValueLabel();
        creditsLabel = createSummaryValueLabel();
        standingLabel = createSummaryValueLabel();
        standingAlertLabel = createSummaryValueLabel();
        standingAlertLabel.setForeground(new Color(220, 38, 38));
        standingAlertLabel.setVisible(false);
        financeTotalLabel = createSummaryValueLabel();
        financePaidLabel = createSummaryValueLabel();
        financeOutstandingLabel = createSummaryValueLabel();
        financeNextDueLabel = createSummaryValueLabel();
        catalogAdvisoryLabel = new JLabel(" ");
        catalogAdvisoryLabel.setForeground(new Color(220, 38, 38));

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        hookListeners();
        refreshProfile();
    }

    private JComponent createHeader() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel title = new JLabel("Student Self Service");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        topRow.add(title, BorderLayout.WEST);
        topRow.add(maintenanceBanner, BorderLayout.EAST);

        container.add(topRow, BorderLayout.NORTH);
        container.add(buildSummaryPanel(), BorderLayout.SOUTH);
        return container;
    }

    private JComponent createBody() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog & Registration", buildCatalogTab());
        tabs.addTab("Timetable", buildScheduleTab());
        tabs.addTab("Grades", new JScrollPane(gradesTable));
        tabs.addTab("Finance", buildFinanceTab());
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
        panel.add(catalogAdvisoryLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildScheduleTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(exportScheduleButton);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);
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

    private JPanel buildSummaryPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel metricsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        metricsRow.setOpaque(false);
        metricsRow.add(buildMetricPanel("Cumulative GPA", gpaLabel));
        metricsRow.add(buildMetricPanel("Credits", creditsLabel));
        metricsRow.add(buildMetricPanel("Standing", standingLabel));

        wrapper.add(metricsRow, BorderLayout.NORTH);

        standingAlertLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel alertRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        alertRow.setOpaque(false);
        alertRow.add(standingAlertLabel);
        wrapper.add(alertRow, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel buildFinanceTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setBorder(BorderFactory.createTitledBorder("Fee Summary"));
        grid.add(buildMetricPanel("Total Fees", financeTotalLabel));
        grid.add(buildMetricPanel("Fees Paid", financePaidLabel));
        grid.add(buildMetricPanel("Outstanding", financeOutstandingLabel));
        grid.add(buildMetricPanel("Next Due Date", financeNextDueLabel));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        reminderButton.setBackground(new Color(59, 130, 246));
        reminderButton.setForeground(Color.WHITE);
        reminderButton.setFocusPainted(false);
        actions.add(reminderButton);

        panel.add(grid, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMetricPanel(String labelText, JLabel valueLabel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText.toUpperCase(Locale.ENGLISH));
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setForeground(new Color(100, 116, 139));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(valueLabel);
        return panel;
    }

    private JLabel createSummaryValueLabel() {
        JLabel label = new JLabel("-");
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(new Color(31, 41, 55));
        return label;
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
        exportScheduleButton.addActionListener(e -> exportSchedule());

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
            updateSummary();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Profile not found", JOptionPane.ERROR_MESSAGE);
            this.studentProfile = null;
            catalogModel.setRowCount(0);
            scheduleModel.setRowCount(0);
            gradesModel.setRowCount(0);
            updateSummary();
            updateActionButtons();
        }
    }

    private void updateMaintenanceState() {
        maintenanceBanner.setText(DatabaseUtil.isMaintenanceMode()
                ? "System is in maintenance mode. Changes disabled."
                : "");
        updateActionButtons();
        updateSummary();
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

            String windowText;
            if (section.getEnrollmentDeadline() == null) {
                windowText = "Open";
            } else if (java.time.LocalDate.now().isAfter(section.getEnrollmentDeadline())) {
                windowText = "Closed";
            } else {
                windowText = "Open until " + section.getEnrollmentDeadline();
            }

            catalogModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId() + " - " + courseTitle,
                    capitalize(section.getDayOfWeek().name()),
                    section.getStartTime().format(TIME_FORMATTER) + "-" + section.getEndTime().format(TIME_FORMATTER),
                    section.getLocation(),
                    availableSeats + "/" + section.getCapacity(),
                    statusText,
                    windowText,
                    prereqText
            });
        }

        updateActionButtons();
        updateSummary();
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

    private void updateSummary() {
        if (studentProfile == null) {
            gpaLabel.setText("-");
            creditsLabel.setText("-");
            standingLabel.setText("-");
            standingAlertLabel.setText("");
            standingAlertLabel.setVisible(false);
            return;
        }

        gpaLabel.setText(String.format(Locale.ENGLISH, "%.2f", studentProfile.getCgpa()));
        creditsLabel.setText(String.format(Locale.ENGLISH, "%d completed / %d in progress",
                studentProfile.getCreditsCompleted(),
                studentProfile.getCreditsInProgress()));

        String standing = studentProfile.getAcademicStanding() != null
                ? studentProfile.getAcademicStanding()
                : studentProfile.getStatus();
        if (standing == null || standing.isBlank()) {
            standing = "Unknown";
        }
        standingLabel.setText(standing);

        if (standing.toLowerCase(Locale.ENGLISH).contains("probation")
                || standing.toLowerCase(Locale.ENGLISH).contains("warning")) {
            standingAlertLabel.setText("Advisory: connect with your advisor to restore good standing.");
            standingAlertLabel.setVisible(true);
        } else {
            standingAlertLabel.setText("");
            standingAlertLabel.setVisible(false);
        }
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
        catalogAdvisoryLabel.setText(" ");

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
        Section section = DatabaseUtil.getSection(sectionId);
        List<String> warnings = new ArrayList<>();

        if (status == EnrollmentRecord.Status.ENROLLED || status == EnrollmentRecord.Status.WAITLISTED) {
            dropButton.setEnabled(true);
            if (status == EnrollmentRecord.Status.WAITLISTED) {
                warnings.add("You are currently waitlisted for this section.");
            } else {
                warnings.add("Already enrolled in this section.");
            }
        }

        boolean canRegister = status == null && section != null;
        if (section != null) {
            if (section.getEnrollmentDeadline() != null
                    && java.time.LocalDate.now().isAfter(section.getEnrollmentDeadline())) {
                warnings.add("Enrollment deadline passed (" + section.getEnrollmentDeadline() + ").");
                canRegister = false;
            }

            List<String> missing = DatabaseUtil.getMissingPrerequisites(studentProfile.getStudentId(), section.getCourseId());
            if (!missing.isEmpty()) {
                warnings.add("Missing prerequisites: " + String.join(", ", missing));
                canRegister = false;
            }

            int projectedCredits = calculateProjectedCredits(section);
            if (projectedCredits > DatabaseUtil.getMaxTermCredits()) {
                warnings.add("Credit load would exceed " + DatabaseUtil.getMaxTermCredits() + " hours.");
                canRegister = false;
            } else if (projectedCredits >= DatabaseUtil.getMaxTermCredits() - 3) {
                warnings.add("Advisor approval may be required for heavy credit load.");
            }
        }

        registerButton.setEnabled(canRegister);
        if (!warnings.isEmpty()) {
            catalogAdvisoryLabel.setText(String.join(" ", warnings));
        } else {
            catalogAdvisoryLabel.setText(" ");
        }
    }

    private int calculateProjectedCredits(Section target) {
        int currentCredits = 0;
        for (Section enrolled : DatabaseUtil.getScheduleForStudent(studentProfile.getStudentId())) {
            Course enrolledCourse = DatabaseUtil.getCourse(enrolled.getCourseId());
            currentCredits += enrolledCourse != null ? Math.max(1, enrolledCourse.getCreditHours()) : 3;
        }
        if (target != null) {
            Course targetCourse = DatabaseUtil.getCourse(target.getCourseId());
            currentCredits += targetCourse != null ? Math.max(1, targetCourse.getCreditHours()) : 3;
        }
        return currentCredits;
    }

    private void exportSchedule() {
        if (studentProfile == null) {
            JOptionPane.showMessageDialog(this, "Student profile unavailable.");
            return;
        }
        List<Section> schedule = DatabaseUtil.getScheduleForStudent(studentProfile.getStudentId());
        if (schedule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No timetable entries to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("timetable_" + studentProfile.getStudentId() + ".ics"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();
        DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String dtStamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .format(Instant.now().atOffset(ZoneOffset.UTC));
        String lineSep = System.lineSeparator();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("BEGIN:VCALENDAR" + lineSep);
            writer.write("VERSION:2.0" + lineSep);
            writer.write("PRODID:-//College ERP//Student Timetable//EN" + lineSep);

            for (Section section : schedule) {
                java.time.LocalDate startDate = java.time.LocalDate.now()
                        .with(TemporalAdjusters.nextOrSame(section.getDayOfWeek()));
                java.time.LocalDateTime startDateTime = java.time.LocalDateTime.of(startDate, section.getStartTime());
                java.time.LocalDateTime endDateTime = java.time.LocalDateTime.of(startDate, section.getEndTime());

                writer.write("BEGIN:VEVENT" + lineSep);
                writer.write("UID:" + UUID.randomUUID() + "@college-erp" + lineSep);
                writer.write("DTSTAMP:" + dtStamp + lineSep);
                writer.write("SUMMARY:" + escapeForIcs(section.getTitle()) + lineSep);
                writer.write("DTSTART:" + startDateTime.format(localFormatter) + lineSep);
                writer.write("DTEND:" + endDateTime.format(localFormatter) + lineSep);
                writer.write("RRULE:FREQ=WEEKLY" + lineSep);
                if (section.getLocation() != null) {
                    writer.write("LOCATION:" + escapeForIcs(section.getLocation()) + lineSep);
                }
                writer.write("DESCRIPTION:" + escapeForIcs(section.getCourseId()) + lineSep);
                writer.write("END:VEVENT" + lineSep);
            }

            writer.write("END:VCALENDAR" + lineSep);
            JOptionPane.showMessageDialog(this, "Timetable exported to " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to export timetable: " + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
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

    private void updateFinanceSummary() {
        if (studentProfile == null) {
            financeTotalLabel.setText("-");
            financePaidLabel.setText("-");
            financeOutstandingLabel.setText("-");
            financeNextDueLabel.setText("-");
            reminderButton.setEnabled(false);
            return;
        }
        financeTotalLabel.setText(String.format("\u20B9%,.0f", studentProfile.getTotalFees()));
        financePaidLabel.setText(String.format("\u20B9%,.0f", studentProfile.getFeesPaid()));
        double outstanding = Math.max(0.0, studentProfile.getTotalFees() - studentProfile.getFeesPaid());
        financeOutstandingLabel.setText(String.format("\u20B9%,.0f", outstanding));
        if (studentProfile.getNextFeeDueDate() != null) {
            financeNextDueLabel.setText(studentProfile.getNextFeeDueDate().toString());
        } else {
            financeNextDueLabel.setText("Not scheduled");
        }
        reminderButton.setEnabled(outstanding > 0.0);
    }

    private void sendPaymentReminder() {
        if (studentProfile == null) {
            return;
        }
        double outstanding = Math.max(0.0, studentProfile.getTotalFees() - studentProfile.getFeesPaid());
        if (outstanding <= 0.0) {
            JOptionPane.showMessageDialog(this, "No outstanding balance.");
            return;
        }

        DatabaseUtil.addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentProfile.getStudentId(),
                "Payment reminder: outstanding balance \u20B9" + String.format("%,.0f", outstanding)
                        + ". Please settle before " + (studentProfile.getNextFeeDueDate() != null
                        ? studentProfile.getNextFeeDueDate()
                        : "the due date"),
                "Finance"));
        updateFinanceSummary();
        JOptionPane.showMessageDialog(this, "Reminder sent to your notifications inbox.");
    }

    private String escapeForIcs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }
}

















package main.java.gui.panels;

import main.java.models.Course;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.FeeInstallment;
import main.java.models.PaymentTransaction;
import main.java.models.User;
import main.java.models.NotificationMessage;
import main.java.service.EnrollmentService;
import main.java.service.StudentService;
import main.java.utils.DatabaseUtil;
import main.java.gui.dialogs.ChangePasswordDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

/**
 * Student-facing self-service workspace for catalog, schedule, and grades.
 */
public class StudentSelfServicePanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

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
    private final JTable paymentHistoryTable;
    private final JTable installmentTable;

    private List<Section> catalogSections = new ArrayList<>();
    private Map<String, EnrollmentRecord.Status> enrollmentStatusBySection = new HashMap<>();
    private final List<String> currentGradeRiskCourses;
    private final JLabel gpaLabel;
    private final JLabel creditsLabel;
    private final JLabel standingLabel;
    private final JLabel standingAlertLabel;
    private final JLabel financeTotalLabel;
    private final JLabel financePaidLabel;
    private final JLabel financeOutstandingLabel;
    private final JLabel financeNextDueLabel;
    private final JLabel catalogAdvisoryLabel;
    private final JLabel gradeAnalyticsLabel;
    private final JButton transcriptPdfButton;
    private final JButton exportSchedulePdfButton;
    private final JButton changePasswordButton;
    private final DefaultTableModel paymentHistoryModel;
    private final DefaultTableModel installmentModel;
    private final DefaultTableModel notificationsModel;
    private final JTable notificationsTable;
    private final JComboBox<String> notificationCategoryFilter;
    private final JCheckBox unreadOnlyCheck;
    private final JButton markReadButton;
    private final JButton markUnreadButton;
    private final JButton refreshNotificationsButton;
    private List<NotificationMessage> notificationsCache = new ArrayList<>();
    private final List<NotificationMessage> filteredNotifications = new ArrayList<>();
    private FeeInstallment nextDueInstallment;

    public StudentSelfServicePanel(User currentUser) {
        this.currentUser = currentUser;
        this.catalogModel = new DefaultTableModel(new Object[]{
                "Section", "Course", "Day", "Time", "Location", "Seats", "Status", "Window", "Prerequisites"
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
        this.paymentHistoryModel = new DefaultTableModel(new Object[]{
                "Date", "Amount", "Method", "Reference", "Notes"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.installmentModel = new DefaultTableModel(new Object[]{
                "Due Date", "Amount", "Status", "Description", "Last Reminder"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.notificationsModel = new DefaultTableModel(new Object[]{
                "Received", "Category", "Status", "Message"
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
        paymentHistoryTable = new JTable(paymentHistoryModel);
        paymentHistoryTable.setRowHeight(22);
        installmentTable = new JTable(installmentModel);
        installmentTable.setRowHeight(22);
        notificationsTable = new JTable(notificationsModel);
        notificationsTable.setRowHeight(22);

        notificationCategoryFilter = new JComboBox<>();
        unreadOnlyCheck = new JCheckBox("Unread only");
        unreadOnlyCheck.setOpaque(false);
        markReadButton = new JButton("Mark as Read");
        markUnreadButton = new JButton("Mark as Unread");
        refreshNotificationsButton = new JButton("Refresh");
        markReadButton.setEnabled(false);
        markUnreadButton.setEnabled(false);

        registerButton = new JButton("Register");
        dropButton = new JButton("Drop");
        transcriptButton = new JButton("Download Transcript (CSV)");
        exportScheduleButton = new JButton("Export Timetable (.ics)");
        exportSchedulePdfButton = new JButton("Print Timetable (PDF)");
        transcriptPdfButton = new JButton("Download Transcript (PDF)");
        changePasswordButton = new JButton("Change Password");
        reminderButton = new JButton("Send Payment Reminder");
        reminderButton.setEnabled(false);

        Color primary = new Color(37, 99, 235);
        exportSchedulePdfButton.setBackground(primary.darker());
        exportSchedulePdfButton.setForeground(Color.WHITE);
        exportSchedulePdfButton.setFocusPainted(false);
        transcriptPdfButton.setBackground(primary.darker());
        transcriptPdfButton.setForeground(Color.WHITE);
        transcriptPdfButton.setFocusPainted(false);
        transcriptButton.setBackground(primary);
        transcriptButton.setForeground(Color.WHITE);
        transcriptButton.setFocusPainted(false);
        exportScheduleButton.setBackground(primary);
        exportScheduleButton.setForeground(Color.WHITE);
        exportScheduleButton.setFocusPainted(false);
        changePasswordButton.setBackground(primary.darker());
        changePasswordButton.setForeground(Color.WHITE);
        changePasswordButton.setFocusPainted(false);
        changePasswordButton.setBorderPainted(false);
        changePasswordButton.setPreferredSize(new Dimension(160, 32));
        Color success = new Color(34, 197, 94);
        Color slate = new Color(100, 116, 139);
        markReadButton.setBackground(success.darker());
        markReadButton.setForeground(Color.WHITE);
        markReadButton.setFocusPainted(false);
        markUnreadButton.setBackground(new Color(249, 115, 22));
        markUnreadButton.setForeground(Color.WHITE);
        markUnreadButton.setFocusPainted(false);
        refreshNotificationsButton.setBackground(slate);
        refreshNotificationsButton.setForeground(Color.WHITE);
        refreshNotificationsButton.setFocusPainted(false);

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
        gradeAnalyticsLabel = new JLabel(" ");
        gradeAnalyticsLabel.setForeground(new Color(71, 85, 105));
        currentGradeRiskCourses = new ArrayList<>();

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
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(changePasswordButton);
        headerActions.add(maintenanceBanner);
        topRow.add(headerActions, BorderLayout.EAST);

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
        tabs.addTab("Notifications", buildNotificationsTab());
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
        controls.add(exportSchedulePdfButton);
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
        transcriptPdfButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(transcriptPdfButton);
        panel.add(Box.createVerticalStrut(10));
        JTextArea info = new JTextArea("""
                Download a CSV or PDF copy of your course grades for personal reference.
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

    private JPanel buildGradesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JScrollPane(gradesTable), BorderLayout.CENTER);
        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        summary.add(gradeAnalyticsLabel);
        panel.add(summary, BorderLayout.SOUTH);
        return panel;
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

        JPanel tables = new JPanel(new GridLayout(1, 2, 12, 12));
        tables.add(wrapWithTitledScroll(paymentHistoryTable, "Payment History"));
        tables.add(wrapWithTitledScroll(installmentTable, "Installment Plan"));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(grid, BorderLayout.NORTH);
        content.add(tables, BorderLayout.CENTER);

        panel.add(content, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildNotificationsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(new JLabel("Category:"));
        notificationCategoryFilter.setPrototypeDisplayValue("Maintenance Alerts    ");
        controls.add(notificationCategoryFilter);
        controls.add(unreadOnlyCheck);
        controls.add(markReadButton);
        controls.add(markUnreadButton);
        controls.add(refreshNotificationsButton);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(notificationsTable), BorderLayout.CENTER);
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

    private JComponent wrapWithTitledScroll(JTable table, String title) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
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
        transcriptPdfButton.addActionListener(e -> exportTranscriptPdf());
        exportScheduleButton.addActionListener(e -> exportScheduleIcs());
        exportSchedulePdfButton.addActionListener(e -> exportSchedulePdf());
        changePasswordButton.addActionListener(e -> showChangePasswordDialog());

        catalogTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        notificationsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateNotificationActions();
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
        notificationCategoryFilter.addActionListener(e -> applyNotificationFilters());
        unreadOnlyCheck.addActionListener(e -> applyNotificationFilters());
        markReadButton.addActionListener(e -> markSelectedNotification(true));
        markUnreadButton.addActionListener(e -> markSelectedNotification(false));
        refreshNotificationsButton.addActionListener(e -> populateNotifications());
    }

    private void showChangePasswordDialog() {
        java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
        JFrame frame = parent instanceof JFrame ? (JFrame) parent : null;
        ChangePasswordDialog dialog = new ChangePasswordDialog(frame, currentUser.getUsername());
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            JOptionPane.showMessageDialog(this, "Password updated successfully.");
        }
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
            updateFinanceSummary();
            populateFinanceTables();
            populateNotifications();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Profile not found", JOptionPane.ERROR_MESSAGE);
            this.studentProfile = null;
            catalogModel.setRowCount(0);
            scheduleModel.setRowCount(0);
            gradesModel.setRowCount(0);
            paymentHistoryModel.setRowCount(0);
            installmentModel.setRowCount(0);
            notificationsModel.setRowCount(0);
            updateSummary();
            updateFinanceSummary();
            notificationsCache = new ArrayList<>();
            notificationCategoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{"All"}));
            unreadOnlyCheck.setSelected(false);
            updateNotificationActions();
            updateActionButtons();
        }
    }

    private void updateMaintenanceState() {
        maintenanceBanner.setText(DatabaseUtil.isMaintenanceMode()
                ? "System is in maintenance mode. Changes disabled."
                : "");
        updateActionButtons();
        updateSummary();
        updateFinanceSummary();
        populateFinanceTables();
        populateNotifications();
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
        updateGradeAnalytics(enrollments);
    }

    private void populateFinanceTables() {
        paymentHistoryModel.setRowCount(0);
        installmentModel.setRowCount(0);
        if (studentProfile == null) {
            return;
        }

        List<PaymentTransaction> history = DatabaseUtil.getPaymentHistoryForStudent(studentProfile.getStudentId());
        for (PaymentTransaction tx : history) {
            paymentHistoryModel.addRow(new Object[]{
                    tx.getPaidOn() != null ? DATE_FORMATTER.format(tx.getPaidOn()) : "-",
                    formatCurrency(tx.getAmount()),
                    tx.getMethod() != null ? tx.getMethod() : "-",
                    tx.getReference() != null ? tx.getReference() : "-",
                    tx.getNotes() != null ? tx.getNotes() : "-"
            });
        }

        List<FeeInstallment> installments = DatabaseUtil.getInstallmentsForStudent(studentProfile.getStudentId());
        LocalDate today = LocalDate.now();
        for (FeeInstallment installment : installments) {
            String statusText;
            if (installment.getStatus() == FeeInstallment.Status.PAID) {
                statusText = "Paid";
            } else if (installment.isOverdue(today)) {
                statusText = "Overdue";
            } else {
                statusText = "Due";
            }
            installmentModel.addRow(new Object[]{
                    installment.getDueDate() != null ? DATE_FORMATTER.format(installment.getDueDate()) : "-",
                    formatCurrency(installment.getAmount()),
                    statusText,
                    installment.getDescription() != null ? installment.getDescription() : "-",
                    installment.getLastReminderSent() != null ? DATE_FORMATTER.format(installment.getLastReminderSent()) : "-"
            });
        }
    }

    private void populateNotifications() {
        notificationsModel.setRowCount(0);
        filteredNotifications.clear();
        if (studentProfile == null) {
            notificationCategoryFilter.setModel(new DefaultComboBoxModel<>(new String[]{"All"}));
            unreadOnlyCheck.setSelected(false);
            updateNotificationActions();
            return;
        }
        String previousSelection = (String) notificationCategoryFilter.getSelectedItem();
        notificationsCache = DatabaseUtil.getNotificationsForStudent(studentProfile.getStudentId());
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        categories.add("All");
        for (NotificationMessage message : notificationsCache) {
            categories.add(normalizeCategory(message.getCategory()));
        }
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(categories.toArray(new String[0]));
        notificationCategoryFilter.setModel(model);
        if (previousSelection != null && categories.stream().anyMatch(cat -> cat.equalsIgnoreCase(previousSelection))) {
            notificationCategoryFilter.setSelectedItem(previousSelection);
        } else {
            notificationCategoryFilter.setSelectedIndex(0);
        }
        applyNotificationFilters();
    }

    private void applyNotificationFilters() {
        notificationsModel.setRowCount(0);
        filteredNotifications.clear();
        notificationsTable.clearSelection();
        if (notificationsCache.isEmpty()) {
            updateNotificationActions();
            return;
        }
        String selectedCategory = (String) notificationCategoryFilter.getSelectedItem();
        if (selectedCategory == null) {
            selectedCategory = "All";
        }
        boolean unreadOnly = unreadOnlyCheck.isSelected();
        for (NotificationMessage message : notificationsCache) {
            String category = normalizeCategory(message.getCategory());
            if (!"All".equalsIgnoreCase(selectedCategory) && !category.equalsIgnoreCase(selectedCategory)) {
                continue;
            }
            if (unreadOnly && message.isRead()) {
                continue;
            }
            filteredNotifications.add(message);
            notificationsModel.addRow(new Object[]{
                    message.getCreatedAt() != null ? DATE_TIME_FORMATTER.format(message.getCreatedAt()) : "-",
                    category,
                    message.isRead() ? "Read" : "Unread",
                    message.getMessage()
            });
        }
        updateNotificationActions();
    }

    private void updateNotificationActions() {
        NotificationMessage selected = getSelectedNotification();
        boolean hasSelection = selected != null;
        markReadButton.setEnabled(hasSelection && !selected.isRead());
        markUnreadButton.setEnabled(hasSelection && selected.isRead());
    }

    private void markSelectedNotification(boolean read) {
        NotificationMessage selected = getSelectedNotification();
        if (selected == null || selected.isRead() == read) {
            return;
        }
        if (selected.getId() == null) {
            return;
        }
        long notificationId = selected.getId();
        DatabaseUtil.markNotificationRead(notificationId, read);
        selected.setRead(read);
        selected.setReadAt(read ? LocalDateTime.now() : null);
        applyNotificationFilters();
        for (int i = 0; i < filteredNotifications.size(); i++) {
            NotificationMessage message = filteredNotifications.get(i);
            if (notificationId == (message.getId() != null ? message.getId() : -1L)) {
                int viewRow = notificationsTable.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    notificationsTable.setRowSelectionInterval(viewRow, viewRow);
                }
                break;
            }
        }
    }

    private NotificationMessage getSelectedNotification() {
        int viewRow = notificationsTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = notificationsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filteredNotifications.size()) {
            return null;
        }
        return filteredNotifications.get(modelRow);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "General";
        }
        String[] parts = category.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(capitalize(parts[i]));
        }
        return builder.toString();
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

    private void updateGradeAnalytics(List<EnrollmentRecord> enrollments) {
        if (studentProfile == null) {
            currentGradeRiskCourses.clear();
            gradeAnalyticsLabel.setText(" ");
            return;
        }
        if (enrollments == null || enrollments.isEmpty()) {
            currentGradeRiskCourses.clear();
            gradeAnalyticsLabel.setText("<html><b>Grade analytics:</b> No graded coursework yet.</html>");
            return;
        }

        double gradedCredits = 0.0;
        double weightedScores = 0.0;
        List<String> riskCourses = new ArrayList<>();
        List<String> pendingCourses = new ArrayList<>();

        for (EnrollmentRecord record : enrollments) {
            if (record.getStatus() != EnrollmentRecord.Status.ENROLLED) {
                continue;
            }

            Section section = DatabaseUtil.getSection(record.getSectionId());
            Course course = null;
            if (section != null && section.getCourseId() != null) {
                course = DatabaseUtil.getCourse(section.getCourseId());
            }
            int credits = course != null ? Math.max(1, course.getCreditHours()) : 3;

            boolean hasFinalGrade = !record.getWeighting().isEmpty() || record.getFinalGrade() > 0.0;
            if (hasFinalGrade) {
                gradedCredits += credits;
                weightedScores += record.getFinalGrade() * credits;
                if (record.getFinalGrade() < 60.0) {
                    riskCourses.add(section != null && section.getCourseId() != null
                            ? section.getCourseId()
                            : record.getSectionId());
                }
            } else {
                pendingCourses.add(section != null && section.getCourseId() != null
                        ? section.getCourseId()
                        : record.getSectionId());
            }
        }

        if (gradedCredits == 0.0 && pendingCourses.isEmpty()) {
            currentGradeRiskCourses.clear();
            gradeAnalyticsLabel.setText("<html><b>Grade analytics:</b> No enrolled coursework yet.</html>");
            return;
        }

        currentGradeRiskCourses.clear();
        currentGradeRiskCourses.addAll(riskCourses);

        StringBuilder builder = new StringBuilder("<html><b>CGPA:</b> ")
                .append(String.format(Locale.ENGLISH, "%.2f", studentProfile.getCgpa()));

        if (gradedCredits > 0.0) {
            double weightedAverage = weightedScores / gradedCredits;
            double termGpa = Math.min(4.0, Math.max(0.0, (weightedAverage / 100.0) * 4.0));
            builder.append(String.format(Locale.ENGLISH, " | <b>Term avg:</b> %.1f%% (~%.2f GPA)",
                    weightedAverage,
                    termGpa));

            double delta = termGpa - studentProfile.getCgpa();
            if (Math.abs(delta) >= 0.05) {
                builder.append(String.format(Locale.ENGLISH,
                        " | <span style='color:%s;'>trend %s %.2f</span>",
                        delta > 0 ? "#16a34a" : "#dc2626",
                        delta > 0 ? "up" : "down",
                        Math.abs(delta)));
            } else {
                builder.append(" | trend steady");
            }
        } else {
            builder.append(" | Term avg pending");
        }

        if (!riskCourses.isEmpty()) {
            builder.append(String.format(Locale.ENGLISH,
                    " | <span style='color:#dc2626;'>At risk: %s</span>",
                    String.join(", ", riskCourses)));
        }

        if (!pendingCourses.isEmpty()) {
            builder.append(String.format(Locale.ENGLISH,
                    " | Pending grades: %s",
                    String.join(", ", pendingCourses)));
        }

        builder.append(" | Standing: ")
                .append(studentProfile.getAcademicStanding() != null
                        ? studentProfile.getAcademicStanding()
                        : "Not set");
        builder.append("</html>");

        gradeAnalyticsLabel.setText(builder.toString());
    }

    private void updateSummary() {
        if (studentProfile == null) {
            gpaLabel.setText("-");
            creditsLabel.setText("-");
            standingLabel.setText("-");
            standingAlertLabel.setText("");
            standingAlertLabel.setVisible(false);
            gradeAnalyticsLabel.setText("<html><b>Grade analytics:</b> Not available.</html>");
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

        if (!currentGradeRiskCourses.isEmpty()) {
            standingAlertLabel.setText("Advisory: focus on " + String.join(", ", currentGradeRiskCourses)
                    + " to stay in good standing.");
            standingAlertLabel.setVisible(true);
        } else if (standing.toLowerCase(Locale.ENGLISH).contains("probation")
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

    private void exportSchedulePdf() {
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
        chooser.setSelectedFile(new java.io.File("timetable_" + studentProfile.getStudentId() + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 48f;
            float lineHeight = 16f;
            float yPosition = page.getMediaBox().getHeight() - margin;

            PDPageContentStream content = null;
            try {
                content = new PDPageContentStream(document, page);
                content.setLeading(lineHeight);
                content.beginText();
                content.newLineAtOffset(margin, yPosition);
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.showText("Weekly Timetable");
                content.newLine();
                yPosition -= lineHeight;
                content.setFont(PDType1Font.HELVETICA, 12);
                String studentLine = "Student: " + studentProfile.getFullName() + " (" + studentProfile.getStudentId() + ")";
                content.showText(studentLine);
                content.newLine();
                yPosition -= lineHeight;
                content.showText("Generated: " + java.time.LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                content.newLine();
                yPosition -= lineHeight;
                content.newLine();
                yPosition -= lineHeight;
                content.showText("Section | Course | Day & Time | Location");
                content.newLine();
                yPosition -= lineHeight;
                content.showText("--------------------------------------------------------------");
                content.newLine();
                yPosition -= lineHeight;

                for (Section section : schedule) {
                    if (yPosition <= margin) {
                        content.endText();
                        content.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        yPosition = page.getMediaBox().getHeight() - margin;
                        content = new PDPageContentStream(document, page);
                        content.setLeading(lineHeight);
                        content.beginText();
                        content.newLineAtOffset(margin, yPosition);
                        content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                        content.showText("Weekly Timetable (cont.)");
                        content.newLine();
                        yPosition -= lineHeight;
                        content.setFont(PDType1Font.HELVETICA, 12);
                        content.showText(studentLine);
                        content.newLine();
                        yPosition -= lineHeight;
                        content.showText("Section | Course | Day & Time | Location");
                        content.newLine();
                        yPosition -= lineHeight;
                        content.showText("--------------------------------------------------------------");
                        content.newLine();
                        yPosition -= lineHeight;
                    }

                    String day = section.getDayOfWeek() != null ? capitalize(section.getDayOfWeek().name()) : "TBA";
                    String timeRange;
                    if (section.getStartTime() != null && section.getEndTime() != null) {
                        timeRange = section.getStartTime().format(TIME_FORMATTER) + "-"
                                + section.getEndTime().format(TIME_FORMATTER);
                    } else {
                        timeRange = "TBA";
                    }
                    String location = section.getLocation() != null ? section.getLocation() : "TBA";
                    String courseTitle = section.getTitle() != null ? section.getTitle() : "";
                    Course course = DatabaseUtil.getCourse(section.getCourseId());
                    if (course != null && (courseTitle == null || courseTitle.isBlank())) {
                        courseTitle = course.getCourseName();
                    }
                    String courseId = section.getCourseId() != null ? section.getCourseId() : "";
                    String courseDisplay = (courseId + " " + (courseTitle != null ? courseTitle : "")).trim();
                    if (courseDisplay.isEmpty()) {
                        courseDisplay = "Course TBD";
                    }
                    String sectionId = section.getSectionId() != null ? section.getSectionId() : "-";
                    String line = String.format(Locale.ENGLISH, "%s | %s | %s %s | %s",
                            sectionId,
                            courseDisplay,
                            day,
                            timeRange,
                            location);
                    content.showText(line);
                    content.newLine();
                    yPosition -= lineHeight;
                }

                content.endText();
            } finally {
                if (content != null) {
                    content.close();
                }
            }

            document.save(file);
            JOptionPane.showMessageDialog(this, "Timetable PDF exported to " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to export timetable PDF: " + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportScheduleIcs() {
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

    private void exportTranscriptPdf() {
        if (studentProfile == null) {
            JOptionPane.showMessageDialog(this, "Student profile unavailable.");
            return;
        }
        List<EnrollmentRecord> records = new ArrayList<>(DatabaseUtil.getEnrollmentsForStudent(studentProfile.getStudentId()));
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transcript data available yet.");
            return;
        }
        records.sort(Comparator.comparing(EnrollmentRecord::getSectionId));

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript_" + studentProfile.getStudentId() + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = chooser.getSelectedFile();

        double totalCredits = 0.0;
        double gradedCredits = 0.0;
        double weightedScores = 0.0;
        List<String> lines = new ArrayList<>();
        lines.add("Section | Course | Credits | Status | Final Score");
        lines.add("---------------------------------------------------------------------");

        for (EnrollmentRecord record : records) {
            Section section = DatabaseUtil.getSection(record.getSectionId());
            Course course = null;
            if (section != null && section.getCourseId() != null) {
                course = DatabaseUtil.getCourse(section.getCourseId());
            }
            int credits = course != null ? Math.max(1, course.getCreditHours()) : 3;
            totalCredits += credits;

            String sectionId = record.getSectionId() != null ? record.getSectionId() : "-";
            String courseName;
            if (course != null && course.getCourseName() != null) {
                courseName = (course.getCourseId() != null ? course.getCourseId() + " " : "") + course.getCourseName();
            } else if (section != null && section.getTitle() != null) {
                courseName = (section.getCourseId() != null ? section.getCourseId() + " " : "") + section.getTitle();
            } else {
                courseName = "Course TBD";
            }
            String statusDisplay = capitalize(record.getStatus().name());
            boolean hasFinalGrade = !record.getWeighting().isEmpty() || record.getFinalGrade() > 0.0;
            String finalScore = hasFinalGrade
                    ? String.format(Locale.ENGLISH, "%.2f", record.getFinalGrade())
                    : "Pending";
            if (hasFinalGrade) {
                gradedCredits += credits;
                weightedScores += record.getFinalGrade() * credits;
            }
            String line = String.format(Locale.ENGLISH, "%s | %s | %d | %s | %s",
                    sectionId,
                    courseName,
                    credits,
                    statusDisplay,
                    finalScore);
            lines.add(line);
        }

        lines.add("");
        lines.add(String.format(Locale.ENGLISH, "Total credits in plan: %.0f", totalCredits));
        if (gradedCredits > 0.0) {
            double weightedAverage = weightedScores / gradedCredits;
            lines.add(String.format(Locale.ENGLISH, "Weighted average score (0-100): %.2f", weightedAverage));
        } else {
            lines.add("Weighted average score (0-100): Pending");
        }
        lines.add(String.format(Locale.ENGLISH, "Reported CGPA: %.2f", studentProfile.getCgpa()));
        lines.add("Academic standing: " + (studentProfile.getAcademicStanding() != null
                ? studentProfile.getAcademicStanding()
                : "Not set"));

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 48f;
            float lineHeight = 16f;
            float yPosition = page.getMediaBox().getHeight() - margin;

            PDPageContentStream content = null;
            try {
                content = new PDPageContentStream(document, page);
                content.setLeading(lineHeight);
                content.beginText();
                content.newLineAtOffset(margin, yPosition);
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.showText("Unofficial Transcript");
                content.newLine();
                yPosition -= lineHeight;
                content.setFont(PDType1Font.HELVETICA, 12);
                content.showText("Student: " + studentProfile.getFullName() + " (" + studentProfile.getStudentId() + ")");
                content.newLine();
                yPosition -= lineHeight;
                content.showText("Program: " + (studentProfile.getCourse() != null ? studentProfile.getCourse() : "Not assigned"));
                content.newLine();
                yPosition -= lineHeight;
                content.showText("Generated: " + java.time.LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                content.newLine();
                yPosition -= lineHeight;
                content.newLine();
                yPosition -= lineHeight;

                for (String line : lines) {
                    if (yPosition <= margin) {
                        content.endText();
                        content.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        yPosition = page.getMediaBox().getHeight() - margin;
                        content = new PDPageContentStream(document, page);
                        content.setLeading(lineHeight);
                        content.beginText();
                        content.newLineAtOffset(margin, yPosition);
                        content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                        content.showText("Unofficial Transcript (cont.)");
                        content.newLine();
                        yPosition -= lineHeight;
                        content.setFont(PDType1Font.HELVETICA, 12);
                    }
                    content.showText(line);
                    content.newLine();
                    yPosition -= lineHeight;
                }

                content.endText();
            } finally {
                if (content != null) {
                    content.close();
                }
            }

            document.save(file);
            JOptionPane.showMessageDialog(this, "Transcript PDF exported to " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to export transcript PDF: " + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateFinanceSummary() {
        if (studentProfile == null) {
            financeTotalLabel.setText("-");
            financePaidLabel.setText("-");
            financeOutstandingLabel.setText("-");
            financeNextDueLabel.setText("-");
            reminderButton.setEnabled(false);
            reminderButton.setToolTipText("No student selected.");
            nextDueInstallment = null;
            return;
        }

        double totalFees = Math.max(0.0, studentProfile.getTotalFees());
        double feesPaid = Math.max(0.0, studentProfile.getFeesPaid());
        double outstanding = Math.max(0.0, totalFees - feesPaid);

        financeTotalLabel.setText(formatCurrency(totalFees));
        financePaidLabel.setText(formatCurrency(feesPaid));
        financeOutstandingLabel.setText(formatCurrency(outstanding));

        nextDueInstallment = DatabaseUtil.nextDueInstallment(studentProfile.getStudentId());
        if (nextDueInstallment != null && nextDueInstallment.getDueDate() != null) {
            boolean overdue = nextDueInstallment.isOverdue(LocalDate.now());
            String statusSuffix = overdue ? " (Overdue)" : "";
            financeNextDueLabel.setText(DATE_FORMATTER.format(nextDueInstallment.getDueDate()) + statusSuffix);
        } else if (outstanding <= 0.0) {
            financeNextDueLabel.setText("Paid in full");
        } else if (studentProfile.getNextFeeDueDate() != null) {
            financeNextDueLabel.setText(studentProfile.getNextFeeDueDate().toString());
        } else {
            financeNextDueLabel.setText("Not scheduled");
        }

        boolean canRemind = outstanding > 0.0 && nextDueInstallment != null;
        reminderButton.setEnabled(canRemind);
        reminderButton.setToolTipText(canRemind
                ? "Send yourself a notification about the next installment."
                : "No pending installment to remind.");
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

        if (nextDueInstallment == null) {
            JOptionPane.showMessageDialog(this, "No upcoming installment to remind.");
            return;
        }

        String dueLabel = nextDueInstallment.getDueDate() != null
                ? DATE_FORMATTER.format(nextDueInstallment.getDueDate())
                : "the upcoming due date";
        String installmentAmount = formatCurrency(nextDueInstallment.getAmount());

        DatabaseUtil.addNotification(new NotificationMessage(
                NotificationMessage.Audience.STUDENT,
                studentProfile.getStudentId(),
                "Payment reminder: " + installmentAmount + " due by " + dueLabel
                        + ". Outstanding balance " + formatCurrency(outstanding) + ".",
                "Finance"));
        DatabaseUtil.markInstallmentReminderSent(studentProfile.getStudentId(), nextDueInstallment.getInstallmentId());
        populateFinanceTables();
        updateFinanceSummary();
        JOptionPane.showMessageDialog(this, "Reminder sent to your notifications inbox.");
    }

    private String formatCurrency(double amount) {
        return String.format("\u20B9%,.0f", amount);
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




















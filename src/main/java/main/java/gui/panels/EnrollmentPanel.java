package main.java.gui.panels;

import main.java.models.Course;
import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.EnrollmentService;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;

import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Panel to help staff manage student registrations, conflicts, and waitlists.
 */
public class EnrollmentPanel extends JPanel implements MaintenanceAware {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private JComboBox<String> studentCombo;
    private JTextField searchField;
    private JTable sectionsTable;
    private JTable scheduleTable;
    private DefaultTableModel sectionsModel;
    private DefaultTableModel scheduleModel;
    private JButton registerButton;
    private JButton dropButton;
    private JButton refreshButton;
    private boolean maintenanceMode;

    private final String[] sectionsColumns = {
        "Section",
        "Course",
        "Faculty",
        "Day & Time",
        "Location",
        "Capacity",
        "Enrolled",
        "Waitlist",
        "Status"
    };

    private final String[] scheduleColumns = {
        "Section",
        "Course",
        "Day & Time",
        "Location"
    };

    private final User actingUser;

    public EnrollmentPanel(User actingUser) {
        this.actingUser = Objects.requireNonNull(actingUser, "actingUser");
        initializeComponents();
        setupLayout();
        setupHandlers();
        loadStudents();
        refreshTables();
    }

    private void initializeComponents() {
        studentCombo = new JComboBox<>();
        searchField = new JTextField(20);
        searchField.setToolTipText("Search sections...");

        sectionsModel = new DefaultTableModel(sectionsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setRowHeight(24);

        scheduleModel = new DefaultTableModel(scheduleColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        scheduleTable = new JTable(scheduleModel);
        scheduleTable.setRowHeight(24);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(sectionsModel);
        sectionsTable.setRowSorter(sorter);

        registerButton = createActionButton("Register", new Color(34, 197, 94));
        dropButton = createActionButton("Drop", new Color(220, 38, 38));
        refreshButton = createActionButton("Refresh", new Color(107, 114, 128));

        dropButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Enrollment Manager");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel studentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        studentPanel.add(new JLabel("Student:"));
        studentPanel.add(studentCombo);

        header.add(title, BorderLayout.WEST);
        header.add(studentPanel, BorderLayout.EAST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(registerButton);
        controls.add(dropButton);
        controls.add(refreshButton);
        controls.add(Box.createHorizontalStrut(20));
        controls.add(new JLabel("Search:"));
        controls.add(searchField);

        JScrollPane sectionScroll = new JScrollPane(sectionsTable);
        sectionScroll.setBorder(BorderFactory.createTitledBorder("Available Sections"));

        JScrollPane scheduleScroll = new JScrollPane(scheduleTable);
        scheduleScroll.setBorder(BorderFactory.createTitledBorder("Current Schedule"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sectionScroll, scheduleScroll);
        splitPane.setResizeWeight(0.65);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
    }

    private void setupHandlers() {
        studentCombo.addActionListener(e -> refreshTables());
        searchField.addActionListener(e -> filterSections());
        refreshButton.addActionListener(e -> refreshTables());

        sectionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        registerButton.addActionListener(e -> registerSelectedSection());
        dropButton.addActionListener(e -> dropSelectedSection());
    }

    private void loadStudents() {
        studentCombo.removeAllItems();
        Collection<Student> students = DatabaseUtil.getAllStudents();
        for (Student student : students) {
            studentCombo.addItem(student.getStudentId() + " - " + student.getFullName());
        }
    }

    private void refreshTables() {
        sectionsModel.setRowCount(0);
        scheduleModel.setRowCount(0);

        if (studentCombo.getSelectedItem() == null) {
            return;
        }

        String studentId = ((String) studentCombo.getSelectedItem()).split(" - ")[0];
        Map<String, EnrollmentRecord.Status> statusMap = buildStatusMap(studentId);

        Collection<Section> sections = DatabaseUtil.getAllSections();
        for (Section section : sections) {
            Course course = DatabaseUtil.getCourse(section.getCourseId());
            Faculty faculty = DatabaseUtil.getFaculty(section.getFacultyId());

            String courseLabel = course != null ? course.getCourseId() : section.getCourseId();
            String facultyLabel = faculty != null ? faculty.getFullName() : section.getFacultyId();
            String when = section.getDayOfWeek().toString().substring(0,3) + " "
                    + section.getStartTime().format(TIME_FORMATTER) + "-"
                    + section.getEndTime().format(TIME_FORMATTER);

            EnrollmentRecord.Status status = statusMap.getOrDefault(section.getSectionId(), null);
            String statusText;
            if (status == EnrollmentRecord.Status.ENROLLED) {
                statusText = "Enrolled";
            } else if (status == EnrollmentRecord.Status.WAITLISTED) {
                statusText = "Waitlisted";
            } else {
                statusText = section.isFull() ? "Full" : "Open";
            }

            sectionsModel.addRow(new Object[]{
                    section.getSectionId(),
                    courseLabel + " • " + section.getTitle(),
                    facultyLabel,
                    when,
                    section.getLocation(),
                    section.getCapacity(),
                    section.getEnrolledStudentIds().size(),
                    section.getWaitlistedStudentIds().size(),
                    statusText
            });
        }

        List<Section> schedule = DatabaseUtil.getScheduleForStudent(studentId);
        for (Section section : schedule) {
            Course course = DatabaseUtil.getCourse(section.getCourseId());
            String courseLabel = course != null ? course.getCourseId() + " • " + section.getTitle()
                    : section.getTitle();
            String when = section.getDayOfWeek().toString().substring(0,3) + " "
                    + section.getStartTime().format(TIME_FORMATTER) + "-"
                    + section.getEndTime().format(TIME_FORMATTER);
            scheduleModel.addRow(new Object[]{
                    section.getSectionId(),
                    courseLabel,
                    when,
                    section.getLocation()
            });
        }
        updateButtonStates();
    }

    private Map<String, EnrollmentRecord.Status> buildStatusMap(String studentId) {
        Map<String, EnrollmentRecord.Status> map = new HashMap<>();
        List<EnrollmentRecord> records = DatabaseUtil.getEnrollmentsForStudent(studentId);
        for (EnrollmentRecord record : records) {
            map.put(record.getSectionId(), record.getStatus());
        }
        return map;
    }

    private void filterSections() {
        String term = searchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter =
                (TableRowSorter<DefaultTableModel>) sectionsTable.getRowSorter();
        if (term.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
        }
    }

    private void registerSelectedSection() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1 || studentCombo.getSelectedItem() == null) {
            return;
        }

        selectedRow = sectionsTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) sectionsModel.getValueAt(selectedRow, 0);
        String studentId = ((String) studentCombo.getSelectedItem()).split(" - ")[0];

        try {
            EnrollmentRecord record = EnrollmentService.registerSection(actingUser, studentId, sectionId);
            if (record.getStatus() == EnrollmentRecord.Status.ENROLLED) {
                JOptionPane.showMessageDialog(this, "Enrollment confirmed!");
            } else {
                JOptionPane.showMessageDialog(this, "Section full. Student waitlisted.");
            }
            refreshTables();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dropSelectedSection() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int selectedRow = sectionsTable.getSelectedRow();
        if (selectedRow == -1 || studentCombo.getSelectedItem() == null) {
            return;
        }

        selectedRow = sectionsTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) sectionsModel.getValueAt(selectedRow, 0);
        String studentId = ((String) studentCombo.getSelectedItem()).split(" - ")[0];

        int option = JOptionPane.showConfirmDialog(
                this,
                "Drop this section for the student?",
                "Confirm Drop",
                JOptionPane.YES_NO_OPTION
        );
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            EnrollmentService.dropSection(actingUser, studentId, sectionId);
            JOptionPane.showMessageDialog(this, "Section dropped.");
            refreshTables();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Drop failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = sectionsTable.getSelectedRow() != -1;
        boolean allowMutations = hasSelection && !maintenanceMode;
        registerButton.setEnabled(allowMutations);
        dropButton.setEnabled(allowMutations);
    }
}

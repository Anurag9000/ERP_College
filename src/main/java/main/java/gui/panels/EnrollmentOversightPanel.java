package main.java.gui.panels;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.AdminService;
import main.java.service.EnrollmentService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin console for overrides, approvals, and waitlist actions.
 */
public class EnrollmentOversightPanel extends JPanel implements MaintenanceAware {
    private final User adminUser;
    private boolean maintenanceMode;

    private final JComboBox<String> studentCombo;
    private final JTextField sectionSearchField;
    private final JTable sectionTable;
    private final JTable studentScheduleTable;
    private final DefaultTableModel sectionModel;
    private final DefaultTableModel studentScheduleModel;
    private final JCheckBox ignoreCapacityCheck;
    private final JCheckBox ignoreConflictsCheck;
    private final JCheckBox ignoreRequisitesCheck;
    private final JCheckBox ignoreCreditsCheck;
    private final JButton forceEnrollButton;
    private final JButton forceDropButton;
    private final JTextField enrollmentDeadlineField;
    private final JTextField dropDeadlineField;
    private final JButton updateDeadlinesButton;

    private final DefaultTableModel approvalsModel;
    private final JTable approvalsTable;
    private final JButton approveRequestButton;
    private final JButton rejectRequestButton;

    private final JComboBox<String> waitlistSectionCombo;
    private final DefaultTableModel waitlistModel;
    private final JTable waitlistTable;
    private final JButton promoteWaitlistButton;
    private final JButton removeWaitlistButton;

    public EnrollmentOversightPanel(User adminUser) {
        this.adminUser = adminUser;
        this.studentCombo = new JComboBox<>();
        this.sectionSearchField = new JTextField(20);
        studentCombo.setPreferredSize(new Dimension(220, 26));

        this.sectionModel = new DefaultTableModel(new Object[] {
                "Section", "Course", "Title", "Day", "Time", "Room", "Capacity", "Enrolled"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.sectionTable = new JTable(sectionModel);
        sectionTable.setRowHeight(22);
        this.studentScheduleModel = new DefaultTableModel(new Object[] {
                "Section", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.studentScheduleTable = new JTable(studentScheduleModel);
        studentScheduleTable.setRowHeight(22);

        this.ignoreCapacityCheck = new JCheckBox("Ignore capacity");
        this.ignoreConflictsCheck = new JCheckBox("Ignore conflicts");
        this.ignoreRequisitesCheck = new JCheckBox("Ignore prerequisites");
        this.ignoreCreditsCheck = new JCheckBox("Ignore credits");
        this.forceEnrollButton = new JButton("Force Enroll");
        this.forceDropButton = new JButton("Force Drop");
        this.enrollmentDeadlineField = new JTextField(10);
        this.dropDeadlineField = new JTextField(10);
        this.updateDeadlinesButton = new JButton("Update Deadlines");

        this.approvalsModel = new DefaultTableModel(new Object[] {
                "Request ID", "Student", "Section", "Requested By", "Created"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.approvalsTable = new JTable(approvalsModel);
        approvalsTable.setRowHeight(22);
        this.approveRequestButton = new JButton("Approve");
        this.rejectRequestButton = new JButton("Reject");

        this.waitlistSectionCombo = new JComboBox<>();
        this.waitlistModel = new DefaultTableModel(new Object[] {
                "Position", "Student ID", "Name", "Approved"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.waitlistTable = new JTable(waitlistModel);
        waitlistTable.setRowHeight(22);
        this.promoteWaitlistButton = new JButton("Promote");
        this.removeWaitlistButton = new JButton("Remove");

        buildLayout();
        hookListeners();
        loadStudents();
        refreshSections();
        refreshStudentSchedule();
        refreshApprovals();
        refreshWaitlistSections();
        refreshWaitlistTable();
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overrides", buildOverridePanel());
        tabs.addTab("Approvals", buildApprovalsPanel());
        tabs.addTab("Waitlists", buildWaitlistPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildOverridePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(new JLabel("Student:"));
        top.add(studentCombo);
        top.add(new JLabel("Filter Sections:"));
        top.add(sectionSearchField);

        JPanel checkboxRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        checkboxRow.add(ignoreCapacityCheck);
        checkboxRow.add(ignoreConflictsCheck);
        checkboxRow.add(ignoreRequisitesCheck);
        checkboxRow.add(ignoreCreditsCheck);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonRow.add(forceEnrollButton);
        buttonRow.add(forceDropButton);

        JPanel deadlineRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        deadlineRow.add(new JLabel("Enrollment deadline (yyyy-MM-dd):"));
        deadlineRow.add(enrollmentDeadlineField);
        deadlineRow.add(new JLabel("Drop deadline:"));
        deadlineRow.add(dropDeadlineField);
        deadlineRow.add(updateDeadlinesButton);

        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.add(top, BorderLayout.NORTH);
        left.add(new JScrollPane(sectionTable), BorderLayout.CENTER);
        JPanel leftSouth = new JPanel(new BorderLayout());
        leftSouth.add(checkboxRow, BorderLayout.NORTH);
        leftSouth.add(buttonRow, BorderLayout.CENTER);
        leftSouth.add(deadlineRow, BorderLayout.SOUTH);
        left.add(leftSouth, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.add(new JLabel("Current Enrollments"), BorderLayout.NORTH);
        right.add(new JScrollPane(studentScheduleTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setResizeWeight(0.65);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildApprovalsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JScrollPane(approvalsTable), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        controls.add(approveRequestButton);
        controls.add(rejectRequestButton);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWaitlistPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        header.add(new JLabel("Section:"));
        waitlistSectionCombo.setPreferredSize(new Dimension(220, 25));
        header.add(waitlistSectionCombo);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshWaitlistTable());
        header.add(refreshButton);
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actions.add(promoteWaitlistButton);
        actions.add(removeWaitlistButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void hookListeners() {
        studentCombo.addActionListener(e -> refreshStudentSchedule());
        sectionSearchField.getDocument().addDocumentListener(new SimpleDocumentListener(this::applySectionFilter));
        sectionTable.getSelectionModel().addListSelectionListener(e -> updateOverrideButtons());
        studentScheduleTable.getSelectionModel().addListSelectionListener(e -> updateOverrideButtons());
        forceEnrollButton.addActionListener(e -> forceEnrollSelected());
        forceDropButton.addActionListener(e -> forceDropSelected());
        updateDeadlinesButton.addActionListener(e -> updateDeadlines());

        approveRequestButton.addActionListener(e -> approveSelectedRequest());
        rejectRequestButton.addActionListener(e -> rejectSelectedRequest());

        waitlistSectionCombo.addActionListener(e -> refreshWaitlistTable());
        promoteWaitlistButton.addActionListener(e -> promoteSelectedWaitlistEntry());
        removeWaitlistButton.addActionListener(e -> removeSelectedWaitlistEntry());
    }

    private void loadStudents() {
        studentCombo.removeAllItems();
        List<Student> students = DatabaseUtil.getAllStudents().stream()
                .sorted(Comparator.comparing(Student::getStudentId))
                .collect(java.util.stream.Collectors.toList());
        for (Student student : students) {
            studentCombo.addItem(student.getStudentId() + " - " + student.getFullName());
        }
    }

    private void refreshSections() {
        sectionModel.setRowCount(0);
        for (Section section : DatabaseUtil.getAllSections()) {
            sectionModel.addRow(new Object[] {
                    section.getSectionId(),
                    section.getCourseId(),
                    section.getTitle(),
                    section.getDayOfWeek(),
                    section.getStartTime() != null ? section.getStartTime() + "-" + section.getEndTime() : "-",
                    section.getLocation(),
                    section.getCapacity(),
                    section.getEnrolledStudentIds().size()
            });
        }
    }

    private void refreshStudentSchedule() {
        studentScheduleModel.setRowCount(0);
        String selection = (String) studentCombo.getSelectedItem();
        if (selection == null) {
            return;
        }
        String studentId = selection.split(" - ")[0];
        for (EnrollmentRecord record : DatabaseUtil.getEnrollmentsForStudent(studentId)) {
            studentScheduleModel.addRow(new Object[] {
                    record.getSectionId(),
                    record.getStatus().name()
            });
        }
        updateOverrideButtons();
    }

    private void applySectionFilter() {
        String term = sectionSearchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(sectionModel);
        sectionTable.setRowSorter(sorter);
        if (term.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
        }
    }

    private void updateOverrideButtons() {
        boolean sectionSelected = sectionTable.getSelectedRow() != -1;
        boolean scheduleSelected = studentScheduleTable.getSelectedRow() != -1;
        boolean enable = !maintenanceMode && sectionSelected && studentCombo.getSelectedItem() != null;
        forceEnrollButton.setEnabled(enable);
        forceDropButton.setEnabled(!maintenanceMode && scheduleSelected && studentCombo.getSelectedItem() != null);
    }

    private void forceEnrollSelected() {
        if (maintenanceMode) {
            return;
        }
        int row = sectionTable.getSelectedRow();
        if (row == -1 || studentCombo.getSelectedItem() == null) {
            return;
        }
        row = sectionTable.convertRowIndexToModel(row);
        String sectionId = (String) sectionModel.getValueAt(row, 0);
        String studentId = ((String) studentCombo.getSelectedItem()).split(" - ")[0];
        try {
            AdminService.overrideEnroll(adminUser,
                    studentId,
                    sectionId,
                    ignoreCapacityCheck.isSelected(),
                    ignoreConflictsCheck.isSelected(),
                    ignoreRequisitesCheck.isSelected(),
                    ignoreCreditsCheck.isSelected());
            JOptionPane.showMessageDialog(this, "Override enrollment completed.");
            refreshStudentSchedule();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Override failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void forceDropSelected() {
        if (maintenanceMode) {
            return;
        }
        int row = studentScheduleTable.getSelectedRow();
        if (row == -1 || studentCombo.getSelectedItem() == null) {
            return;
        }
        String sectionId = (String) studentScheduleModel.getValueAt(row, 0);
        String studentId = ((String) studentCombo.getSelectedItem()).split(" - ")[0];
        try {
            DatabaseUtil.dropStudentFromSection(adminUser.getUsername(), studentId, sectionId);
            JOptionPane.showMessageDialog(this, "Student dropped from section.");
            refreshStudentSchedule();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Drop failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDeadlines() {
        if (maintenanceMode) {
            return;
        }
        int row = sectionTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        row = sectionTable.convertRowIndexToModel(row);
        String sectionId = (String) sectionModel.getValueAt(row, 0);
        LocalDate enrollmentDate = parseDate(enrollmentDeadlineField.getText().trim());
        LocalDate dropDate = parseDate(dropDeadlineField.getText().trim());
        try {
            AdminService.updateSectionDeadlines(adminUser, sectionId, enrollmentDate, dropDate);
            JOptionPane.showMessageDialog(this, "Deadlines updated.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update deadlines",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim());
    }

    private void refreshApprovals() {
        approvalsModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        for (DatabaseUtil.RegistrationRequestView view : DatabaseUtil.getPendingRegistrationRequests()) {
            approvalsModel.addRow(new Object[] {
                    view.id(),
                    view.studentId() + " - " + view.studentName(),
                    view.sectionId(),
                    view.requestedBy(),
                    view.requestedAt().atZone(java.time.ZoneId.systemDefault()).format(formatter)
            });
        }
    }

    private void approveSelectedRequest() {
        if (maintenanceMode) {
            return;
        }
        int row = approvalsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        long requestId = ((Number) approvalsModel.getValueAt(row, 0)).longValue();
        try {
            DatabaseUtil.approveRegistrationRequest(adminUser, requestId, "Approved via oversight panel");
            refreshApprovals();
            JOptionPane.showMessageDialog(this, "Request approved.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to approve", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rejectSelectedRequest() {
        if (maintenanceMode) {
            return;
        }
        int row = approvalsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        long requestId = ((Number) approvalsModel.getValueAt(row, 0)).longValue();
        String notes = JOptionPane.showInputDialog(this, "Reason for rejection:");
        if (notes == null) {
            return;
        }
        try {
            DatabaseUtil.rejectRegistrationRequest(adminUser, requestId, notes);
            refreshApprovals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to reject", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshWaitlistSections() {
        waitlistSectionCombo.removeAllItems();
        for (Section section : DatabaseUtil.getAllSections()) {
            waitlistSectionCombo.addItem(section.getSectionId() + " - " + section.getTitle());
        }
    }

    private void refreshWaitlistTable() {
        waitlistModel.setRowCount(0);
        String selection = (String) waitlistSectionCombo.getSelectedItem();
        if (selection == null) {
            return;
        }
        String sectionId = selection.split(" - ")[0];
        for (DatabaseUtil.WaitlistSnapshot entry : DatabaseUtil.getWaitlistSnapshot(sectionId)) {
            waitlistModel.addRow(new Object[] {
                    entry.position(),
                    entry.studentId(),
                    entry.studentName(),
                    entry.approved() ? "Yes" : "No"
            });
        }
    }

    private void promoteSelectedWaitlistEntry() {
        if (maintenanceMode) {
            return;
        }
        int row = waitlistTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        String sectionId = getSelectedWaitlistSectionId();
        String studentId = (String) waitlistModel.getValueAt(row, 1);
        try {
            AdminService.promoteWaitlisted(adminUser, sectionId, studentId);
            JOptionPane.showMessageDialog(this, "Waitlist entry promoted.");
            refreshWaitlistTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to promote", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedWaitlistEntry() {
        if (maintenanceMode) {
            return;
        }
        int row = waitlistTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        String sectionId = getSelectedWaitlistSectionId();
        String studentId = (String) waitlistModel.getValueAt(row, 1);
        try {
            AdminService.removeWaitlistEntry(adminUser, sectionId, studentId);
            JOptionPane.showMessageDialog(this, "Waitlist entry removed.");
            refreshWaitlistTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to remove entry", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getSelectedWaitlistSectionId() {
        String selection = (String) waitlistSectionCombo.getSelectedItem();
        return selection != null ? selection.split(" - ")[0] : "";
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        updateOverrideButtons();
        approveRequestButton.setEnabled(!maintenance);
        rejectRequestButton.setEnabled(!maintenance);
        promoteWaitlistButton.setEnabled(!maintenance);
        removeWaitlistButton.setEnabled(!maintenance);
        updateDeadlinesButton.setEnabled(!maintenance);
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable callback;

        SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            callback.run();
        }
    }
}

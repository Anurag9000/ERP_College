package main.java.gui.panels;

import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.gui.dialogs.StudentDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
// unused imports removed
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import javax.swing.RowFilter;

/**
 * Panel for managing student information
 */
public class StudentPanel extends JPanel implements MaintenanceAware {
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton, scheduleButton;
    private boolean maintenanceMode;

    private final String[] columnNames = {
            "Student ID", "Username", "Name", "Email", "Phone", "Course",
            "Semester", "Status", "CGPA", "Progress", "Fees Paid", "Outstanding", "Next Due"
    };

    public StudentPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadStudentData();
    }

    private void initializeComponents() {
        // Table
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(25);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        studentTable.setRowSorter(sorter);

        // Search field
        searchField = new JTextField(20);
        searchField.setToolTipText("Search students...");

        // Buttons
        addButton = new JButton("Add Student");
        editButton = new JButton("Edit Student");
        deleteButton = new JButton("Delete Student");
        scheduleButton = new JButton("View Schedule");
        refreshButton = new JButton("Refresh");

        // Style buttons
        Color primaryColor = new Color(37, 99, 235);
        Color successColor = new Color(34, 197, 94);
        Color dangerColor = new Color(220, 38, 38);

        addButton.setBackground(successColor);
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);

        editButton.setBackground(primaryColor);
        editButton.setForeground(Color.WHITE);
        editButton.setFocusPainted(false);

        deleteButton.setBackground(dangerColor);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);

        scheduleButton.setBackground(new Color(8, 145, 178));
        scheduleButton.setForeground(Color.WHITE);
        scheduleButton.setFocusPainted(false);

        refreshButton.setBackground(new Color(107, 114, 128));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);

        // Initially disable edit and delete buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        scheduleButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Student Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(scheduleButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(refreshButton);

        // Table panel
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setPreferredSize(new Dimension(0, 400));

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        // Table selection
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Double-click to edit
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && studentTable.getSelectedRow() != -1) {
                    editStudent();
                }
            }
        });

        // Search functionality
        searchField.addActionListener(e -> filterTable());

        // Button actions
        addButton.addActionListener(e -> addStudent());
        editButton.addActionListener(e -> editStudent());
        deleteButton.addActionListener(e -> deleteStudent());
        scheduleButton.addActionListener(e -> viewSchedule());
        refreshButton.addActionListener(e -> loadStudentData());
    }

    private void loadStudentData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        addButton.setEnabled(false);
        refreshButton.setEnabled(false);

        new SwingWorker<Collection<Student>, Void>() {
            @Override
            protected Collection<Student> doInBackground() {
                return DatabaseUtil.getAllStudents();
            }

            @Override
            protected void done() {
                try {
                    Collection<Student> students = get();
                    tableModel.setRowCount(0);

                    for (Student student : students) {
                        Object[] row = {
                                student.getStudentId(),
                                student.getUsername(),
                                student.getFullName(),
                                student.getEmail(),
                                student.getPhone(),
                                student.getCourse(),
                                student.getSemester(),
                                student.getStatus(),
                                String.format("%.2f", student.getCgpa()),
                                String.format("%.0f%%", student.getProgressPercent()),
                                "₹" + String.format("%.0f", student.getFeesPaid()),
                                "₹" + String.format("%.0f", student.getOutstandingFees()),
                                student.getNextFeeDueDate() != null
                                        ? student.getNextFeeDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                        : "-"
                        };
                        tableModel.addRow(row);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentPanel.this, "Error loading students: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    refreshButton.setEnabled(true);
                    updateButtonStates();
                }
            }
        }.execute();
    }

    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) studentTable.getRowSorter();

        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void viewSchedule() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Student student = DatabaseUtil.getStudent(studentId);
                java.util.List<main.java.models.Section> schedule = DatabaseUtil.getScheduleForStudent(studentId);

                if (schedule.isEmpty()) {
                    return "No active sections for " + student.getFullName() + ".";
                }

                StringBuilder builder = new StringBuilder();
                builder.append("Schedule for ").append(student.getFullName()).append(":\n\n");
                for (main.java.models.Section section : schedule) {
                    builder.append(section.getSectionId())
                            .append(" • ")
                            .append(section.getTitle())
                            .append(" • ")
                            .append(section.getDayOfWeek().toString().substring(0, 3)).append(" ")
                            .append(section.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append("-").append(section.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append(" • ").append(section.getLocation())
                            .append("\n");
                }
                return builder.toString();
            }

            @Override
            protected void done() {
                try {
                    String message = get();
                    JOptionPane.showMessageDialog(StudentPanel.this, message, "Student Schedule",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentPanel.this, "Error returning schedule: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void addStudent() {
        if (blockIfMaintenance())
            return;

        StudentDialog dialog = new StudentDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Add Student",
                null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Student student = dialog.getStudent();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    User linkedUser = DatabaseUtil.getUser(student.getUsername());
                    if (linkedUser == null || !"Student".equalsIgnoreCase(linkedUser.getRole())) {
                        throw new Exception("Create a student user account before adding the profile.");
                    }
                    DatabaseUtil.addStudent(student);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(StudentPanel.this, "Student added successfully!");
                        loadStudentData();
                    } catch (Exception e) {
                        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(StudentPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void editStudent() {
        if (blockIfMaintenance())
            return;

        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);

        // Fetch student first (blocking but fast enough usually, or ideally async too
        // but dialog needs it)
        // Let's do a quick fetch on EDT for the dialog populating, assuming getStudent
        // is fast map lookup
        // actually DatabaseUtil.getStudent might be fast map lookup, but good practice
        // is async.
        // For editing, we need the object to show the dialog.

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Student, Void>() {
            @Override
            protected Student doInBackground() {
                return DatabaseUtil.getStudent(studentId);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Student student = get();
                    if (student != null) {
                        showEditDialog(student);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentPanel.this, "Error: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                }
            }
        }.execute();
    }

    private void showEditDialog(Student student) {
        StudentDialog dialog = new StudentDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Edit Student",
                student);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Student updatedStudent = dialog.getStudent();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    User linkedUser = DatabaseUtil.getUser(updatedStudent.getUsername());
                    // Validation logic...
                    if (linkedUser == null || !"Student".equalsIgnoreCase(linkedUser.getRole())) {
                        throw new Exception("Username must correspond to an existing student user.");
                    }
                    DatabaseUtil.updateStudent(updatedStudent);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(StudentPanel.this, "Student updated successfully!");
                        loadStudentData();
                    } catch (Exception e) {
                        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        JOptionPane.showMessageDialog(StudentPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void deleteStudent() {
        if (blockIfMaintenance())
            return;

        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);
        String studentName = (String) tableModel.getValueAt(selectedRow, 2);

        int option = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete student: " + studentName + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DatabaseUtil.deleteStudent(studentId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(StudentPanel.this, "Student deleted successfully!");
                        loadStudentData();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(StudentPanel.this, "Error deleting: " + e.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean maintenance = isMaintenanceLocked();
        addButton.setEnabled(!maintenance);
        boolean hasSelection = studentTable.getSelectedRow() != -1;
        editButton.setEnabled(hasSelection && !maintenance);
        deleteButton.setEnabled(hasSelection && !maintenance);
        scheduleButton.setEnabled(hasSelection);
    }

    private boolean blockIfMaintenance() {
        if (isMaintenanceLocked()) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return true;
        }
        return false;
    }

    private boolean isMaintenanceLocked() {
        boolean maintenance = maintenanceMode || DatabaseUtil.isMaintenanceMode();
        if (maintenanceMode != maintenance) {
            maintenanceMode = maintenance;
        }
        return maintenance;
    }
}

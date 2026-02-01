package main.java.gui.panels;

import main.java.models.Course;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;
import javax.swing.*;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collection;
import javax.swing.RowFilter;

/**
 * Panel for managing course information
 */
public class CoursePanel extends JPanel implements MaintenanceAware {
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton;
    private boolean maintenanceMode;

    private final String[] columnNames = {
            "Course ID", "Course Name", "Department", "Duration (Sem)",
            "Fees", "Total Seats", "Available Seats", "Enrolled", "Status"
    };

    public CoursePanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadCourseData();
    }

    private void initializeComponents() {
        // Table
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        courseTable = new JTable(tableModel);
        courseTable.setRowHeight(25);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        courseTable.setRowSorter(sorter);

        // Search field
        searchField = new JTextField(20);
        searchField.setToolTipText("Search courses...");

        // Buttons
        addButton = new JButton("Add Course");
        editButton = new JButton("Edit Course");
        deleteButton = new JButton("Delete Course");
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

        refreshButton.setBackground(new Color(107, 114, 128));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);

        // Initially disable edit and delete buttons
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Course Management");
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
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(refreshButton);

        // Table panel
        JScrollPane scrollPane = new JScrollPane(courseTable);
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
        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Search functionality
        searchField.addActionListener(e -> filterTable());

        // Button actions
        addButton.addActionListener(e -> addCourse());
        editButton.addActionListener(e -> editCourse());
        deleteButton.addActionListener(e -> deleteCourse());
        refreshButton.addActionListener(e -> loadCourseData());
    }

    private void loadCourseData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Collection<Course>, Void>() {
            @Override
            protected Collection<Course> doInBackground() throws Exception {
                return DatabaseUtil.getAllCourses();
            }

            @Override
            protected void done() {
                try {
                    Collection<Course> courses = get();
                    tableModel.setRowCount(0);
                    for (Course course : courses) {
                        Object[] row = {
                                course.getCourseId(),
                                course.getCourseName(),
                                course.getDepartment(),
                                course.getDuration(),
                                course.getCreditHours(),
                                String.format("\u20B9%,.0f", course.getFees()),
                                course.getTotalSeats(),
                                course.getAvailableSeats(),
                                course.getEnrolledStudents(),
                                course.getStatus()
                        };
                        tableModel.addRow(row);
                    }
                    updateButtonStates();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CoursePanel.this,
                            "Error loading courses: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) courseTable.getRowSorter();

        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void addCourse() {
        if (blockIfMaintenance()) {
            return;
        }
        Course edited = showCourseDialog(null);
        if (edited != null) {
            addButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseUtil.addCourse(edited);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadCourseData();
                        JOptionPane.showMessageDialog(CoursePanel.this, "Course created.");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(CoursePanel.this, "Error creating course: " + e.getMessage());
                    } finally {
                        addButton.setEnabled(true);
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void editCourse() {
        if (blockIfMaintenance()) {
            return;
        }
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedRow = courseTable.convertRowIndexToModel(selectedRow);
        String courseId = (String) tableModel.getValueAt(selectedRow, 0);

        // Fetch raw object quickly or in background?
        // Typically a single fetch by ID is fast, but let's be safe.
        // However, showCourseDialog needs the object.
        // So we'll run a worker to fetch -> show dialog -> run worker to save.

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Course, Void>() {
            @Override
            protected Course doInBackground() throws Exception {
                return DatabaseUtil.getCourse(courseId);
            }

            @Override
            protected void done() {
                try {
                    Course existing = get();
                    setCursor(Cursor.getDefaultCursor());

                    if (existing == null) {
                        JOptionPane.showMessageDialog(CoursePanel.this, "Unable to load course.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Course edited = showCourseDialog(existing);
                    if (edited != null) {
                        saveEditedCourse(edited);
                    }
                } catch (Exception e) {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(CoursePanel.this, "Error loading course details: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void saveEditedCourse(Course edited) {
        editButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatabaseUtil.updateCourse(edited);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadCourseData();
                    JOptionPane.showMessageDialog(CoursePanel.this, "Course updated.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CoursePanel.this, "Error updating course: " + e.getMessage());
                } finally {
                    editButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void deleteCourse() {
        if (blockIfMaintenance()) {
            return;
        }
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = courseTable.convertRowIndexToModel(selectedRow);
        String courseId = (String) tableModel.getValueAt(selectedRow, 0);
        String courseName = (String) tableModel.getValueAt(selectedRow, 1);

        int option = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete course: " + courseName + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            deleteButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseUtil.deleteCourse(courseId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadCourseData();
                        JOptionPane.showMessageDialog(CoursePanel.this, "Course deleted successfully!");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(CoursePanel.this, "Error deleting course: " + e.getMessage());
                    } finally {
                        deleteButton.setEnabled(true);
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
        boolean hasSelection = courseTable.getSelectedRow() != -1;
        editButton.setEnabled(hasSelection && !maintenance);
        deleteButton.setEnabled(hasSelection && !maintenance);
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

    private Course showCourseDialog(Course course) {
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(20);
        JTextField deptField = new JTextField(20);
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 12, 1));
        JSpinner creditSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 30, 1));
        JTextField feesField = new JTextField(10);
        JTextField seatsField = new JTextField(5);
        JTextArea descArea = new JTextArea(4, 20);
        JTextArea subjectsArea = new JTextArea(3, 20);

        if (course == null) {
            String nextId = DatabaseUtil.generateNextId("CRS", DatabaseUtil.getAllCourses());
            idField.setText(nextId);
        } else {
            idField.setText(course.getCourseId());
            nameField.setText(course.getCourseName());
            deptField.setText(course.getDepartment());
            durationSpinner.setValue(course.getDuration());
            creditSpinner.setValue(course.getCreditHours());
            feesField.setText(String.valueOf(course.getFees()));
            seatsField.setText(String.valueOf(course.getTotalSeats()));
            descArea.setText(course.getDescription());
            subjectsArea.setText(String.join(", ", course.getSubjects()));
        }
        idField.setEditable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Course ID:"), gbc);
        gbc.gridx = 1;
        panel.add(idField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        panel.add(deptField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Duration (semesters):"), gbc);
        gbc.gridx = 1;
        panel.add(durationSpinner, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Credit Hours:"), gbc);
        gbc.gridx = 1;
        panel.add(creditSpinner, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Fees:"), gbc);
        gbc.gridx = 1;
        panel.add(feesField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Total Seats:"), gbc);
        gbc.gridx = 1;
        panel.add(seatsField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(descArea), gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Subjects (comma separated):"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(subjectsArea), gbc);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                course == null ? "Add Course" : "Edit Course",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Course name is required.");
            return null;
        }

        double fees;
        int seats;
        try {
            fees = Double.parseDouble(feesField.getText().trim());
            seats = Integer.parseInt(seatsField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid fees or seat count.");
            return null;
        }

        Course edited = course == null
                ? new Course(idField.getText().trim(), nameField.getText().trim(),
                        deptField.getText().trim(), (Integer) durationSpinner.getValue(), fees,
                        descArea.getText().trim(), seats)
                : course;

        if (course != null) {
            edited.setCourseName(nameField.getText().trim());
            edited.setDepartment(deptField.getText().trim());
            edited.setDuration((Integer) durationSpinner.getValue());
            edited.setFees(fees);
            edited.setDescription(descArea.getText().trim());
            edited.setTotalSeats(seats);
            edited.setAvailableSeats(Math.max(0, seats - edited.getEnrolledStudents()));
        }
        edited.setCreditHours((Integer) creditSpinner.getValue());

        java.util.List<String> subjects = new java.util.ArrayList<>();
        for (String token : subjectsArea.getText().split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                subjects.add(trimmed);
            }
        }
        edited.setSubjects(subjects);
        return edited;
    }
}

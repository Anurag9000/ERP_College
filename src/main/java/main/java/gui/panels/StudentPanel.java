package main.java.gui.panels;

import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.gui.dialogs.StudentDialog;
import main.java.gui.panels.MaintenanceAware;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        tableModel.setRowCount(0);
        Collection<Student> students = DatabaseUtil.getAllStudents();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
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
                student.getNextFeeDueDate() != null ? student.getNextFeeDueDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "-"
            };
            tableModel.addRow(row);
        }
        updateButtonStates();
    }
    
    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = 
            (TableRowSorter<DefaultTableModel>) studentTable.getRowSorter();
        
        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void viewSchedule() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);
        Student student = DatabaseUtil.getStudent(studentId);

        java.util.List<main.java.models.Section> schedule = DatabaseUtil.getScheduleForStudent(studentId);
        if (schedule.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No active sections for " + student.getFullName() + ".");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Schedule for ").append(student.getFullName()).append(":\n\n");
        for (main.java.models.Section section : schedule) {
            builder.append(section.getSectionId())
                    .append(" • ")
                    .append(section.getTitle())
                    .append(" • ")
                    .append(section.getDayOfWeek().toString().substring(0,3)).append(" ")
                    .append(section.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append("-").append(section.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append(" • ").append(section.getLocation())
                    .append("\n");
        }

        JOptionPane.showMessageDialog(this, builder.toString(), "Student Schedule", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void addStudent() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        StudentDialog dialog = new StudentDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), 
            "Add Student", 
            null
        );
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            Student student = dialog.getStudent();
            User linkedUser = DatabaseUtil.getUser(student.getUsername());
            if (linkedUser == null || !"Student".equalsIgnoreCase(linkedUser.getRole())) {
                JOptionPane.showMessageDialog(this,
                        "Create a student user account before adding the profile.",
                        "Missing user",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseUtil.addStudent(student);
            loadStudentData();
            JOptionPane.showMessageDialog(this, "Student added successfully!");
        }
    }
    
    private void editStudent() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        // Convert view row to model row
        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);
        Student student = DatabaseUtil.getStudent(studentId);
        
        if (student != null) {
            StudentDialog dialog = new StudentDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), 
                "Edit Student", 
                student
            );
            dialog.setVisible(true);
            
            if (dialog.isConfirmed()) {
                Student updatedStudent = dialog.getStudent();
                User linkedUser = DatabaseUtil.getUser(updatedStudent.getUsername());
                if (linkedUser == null || !"Student".equalsIgnoreCase(linkedUser.getRole())) {
                    JOptionPane.showMessageDialog(this,
                            "Username must correspond to an existing student user.",
                            "Invalid username",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                DatabaseUtil.updateStudent(updatedStudent);
                loadStudentData();
                JOptionPane.showMessageDialog(this, "Student updated successfully!");
            }
        }
    }
    
    private void deleteStudent() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        selectedRow = studentTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);
        String studentName = (String) tableModel.getValueAt(selectedRow, 2);
        
        int option = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete student: " + studentName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            DatabaseUtil.deleteStudent(studentId);
            loadStudentData();
            JOptionPane.showMessageDialog(this, "Student deleted successfully!");
        }
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        updateButtonStates();
    }

    private void updateButtonStates() {
        addButton.setEnabled(!maintenanceMode);
        boolean hasSelection = studentTable.getSelectedRow() != -1;
        editButton.setEnabled(hasSelection && !maintenanceMode);
        deleteButton.setEnabled(hasSelection && !maintenanceMode);
        scheduleButton.setEnabled(hasSelection);
    }
}

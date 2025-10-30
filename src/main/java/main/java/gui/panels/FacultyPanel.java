package main.java.gui.panels;

import main.java.gui.dialogs.FacultyDialog;
import main.java.models.Faculty;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collection;
import javax.swing.RowFilter;

/**
 * Panel for managing faculty information
 */
public class FacultyPanel extends JPanel {
    private JTable facultyTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton;
    
    private final String[] columnNames = {
        "Faculty ID", "Username", "Name", "Department", "Designation",
        "Email", "Phone", "Qualification", "Salary", "Status"
    };
    
    public FacultyPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadFacultyData();
    }
    
    private void initializeComponents() {
        // Table
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        facultyTable = new JTable(tableModel);
        facultyTable.setRowHeight(25);
        facultyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        facultyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        facultyTable.setRowSorter(sorter);
        
        // Search field
        searchField = new JTextField(20);
        searchField.setToolTipText("Search faculty...");
        
        // Buttons
        addButton = new JButton("Add Faculty");
        editButton = new JButton("Edit Faculty");
        deleteButton = new JButton("Delete Faculty");
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
        
        JLabel titleLabel = new JLabel("Faculty Management");
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
        JScrollPane scrollPane = new JScrollPane(facultyTable);
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
        facultyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = facultyTable.getSelectedRow() != -1;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
            }
        });
        
        // Search functionality
        searchField.addActionListener(e -> filterTable());
        
        // Button actions
        addButton.addActionListener(e -> addFaculty());
        editButton.addActionListener(e -> editFaculty());
        deleteButton.addActionListener(e -> deleteFaculty());
        refreshButton.addActionListener(e -> loadFacultyData());
    }
    
    private void loadFacultyData() {
        tableModel.setRowCount(0);
        Collection<Faculty> faculties = DatabaseUtil.getAllFaculty();
        
        for (Faculty faculty : faculties) {
            Object[] row = {
                faculty.getFacultyId(),
                faculty.getUsername(),
                faculty.getFullName(),
                faculty.getDepartment(),
                faculty.getDesignation(),
                faculty.getEmail(),
                faculty.getPhone(),
                faculty.getQualification(),
                "₹" + String.format("%.0f", faculty.getSalary()),
                faculty.getStatus()
            };
            tableModel.addRow(row);
        }
    }
    
    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = 
            (TableRowSorter<DefaultTableModel>) facultyTable.getRowSorter();
        
        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }
    
    private void addFaculty() {
        FacultyDialog dialog = new FacultyDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Add Faculty", null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Faculty faculty = dialog.getFaculty();
            User linkedUser = DatabaseUtil.getUser(faculty.getUsername());
            if (linkedUser == null || !"Instructor".equalsIgnoreCase(linkedUser.getRole())) {
                JOptionPane.showMessageDialog(this,
                        "Create an instructor user before adding faculty profile.",
                        "Missing user",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseUtil.addFaculty(faculty);
            loadFacultyData();
        }
    }
    
    private void editFaculty() {
        int selectedRow = facultyTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedRow = facultyTable.convertRowIndexToModel(selectedRow);
        String facultyId = (String) tableModel.getValueAt(selectedRow, 0);
        Faculty faculty = DatabaseUtil.getFaculty(facultyId);
        if (faculty == null) {
            JOptionPane.showMessageDialog(this, "Unable to load faculty profile.");
            return;
        }
        FacultyDialog dialog = new FacultyDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Edit Faculty", faculty);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Faculty updated = dialog.getFaculty();
            User linkedUser = DatabaseUtil.getUser(updated.getUsername());
            if (linkedUser == null || !"Instructor".equalsIgnoreCase(linkedUser.getRole())) {
                JOptionPane.showMessageDialog(this,
                        "Username must correspond to an instructor user.",
                        "Invalid username",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseUtil.updateFaculty(updated);
            loadFacultyData();
        }
    }
    
    private void deleteFaculty() {
        int selectedRow = facultyTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        selectedRow = facultyTable.convertRowIndexToModel(selectedRow);
        String facultyId = (String) tableModel.getValueAt(selectedRow, 0);
        String facultyName = (String) tableModel.getValueAt(selectedRow, 2);
        
        int option = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete faculty: " + facultyName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            DatabaseUtil.deleteFaculty(facultyId);
            loadFacultyData();
            JOptionPane.showMessageDialog(this, "Faculty deleted successfully!");
        }
    }
}

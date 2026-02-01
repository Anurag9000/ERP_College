package main.java.gui.panels;

import main.java.gui.dialogs.FacultyDialog;
import main.java.models.Faculty;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collection;
import javax.swing.RowFilter;

/**
 * Panel for managing faculty information
 */
public class FacultyPanel extends JPanel implements MaintenanceAware {
    private JTable facultyTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton, editButton, deleteButton, refreshButton;
    private boolean maintenanceMode;

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
                updateButtonStates();
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
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Collection<Faculty>, Void>() {
            @Override
            protected Collection<Faculty> doInBackground() throws Exception {
                return DatabaseUtil.getAllFaculty();
            }

            @Override
            protected void done() {
                try {
                    Collection<Faculty> faculties = get();
                    tableModel.setRowCount(0);
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
                    updateButtonStates();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FacultyPanel.this,
                            "Error loading faculty: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) facultyTable.getRowSorter();

        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }

    private void addFaculty() {
        if (blockIfMaintenance()) {
            return;
        }
        FacultyDialog dialog = new FacultyDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Add Faculty", null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Faculty faculty = dialog.getFaculty();

            addButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    User linkedUser = DatabaseUtil.getUser(faculty.getUsername());
                    if (linkedUser == null || !"Instructor".equalsIgnoreCase(linkedUser.getRole())) {
                        throw new IllegalArgumentException("Create an instructor user (" + faculty.getUsername()
                                + ") before adding faculty profile.");
                    }
                    DatabaseUtil.addFaculty(faculty);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadFacultyData();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(FacultyPanel.this,
                                e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        addButton.setEnabled(true);
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void editFaculty() {
        if (blockIfMaintenance()) {
            return;
        }
        int selectedRow = facultyTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedRow = facultyTable.convertRowIndexToModel(selectedRow);
        String facultyId = (String) tableModel.getValueAt(selectedRow, 0);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Faculty, Void>() {
            @Override
            protected Faculty doInBackground() throws Exception {
                return DatabaseUtil.getFaculty(facultyId);
            }

            @Override
            protected void done() {
                try {
                    Faculty faculty = get();
                    setCursor(Cursor.getDefaultCursor());

                    if (faculty == null) {
                        JOptionPane.showMessageDialog(FacultyPanel.this, "Unable to load faculty profile.");
                        return;
                    }

                    FacultyDialog dialog = new FacultyDialog(
                            (JFrame) SwingUtilities.getWindowAncestor(FacultyPanel.this), "Edit Faculty", faculty);
                    dialog.setVisible(true);

                    if (dialog.isConfirmed()) {
                        saveEditedFaculty(dialog.getFaculty());
                    }
                } catch (Exception e) {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(FacultyPanel.this, "Error loading faculty: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void saveEditedFaculty(Faculty updated) {
        editButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                User linkedUser = DatabaseUtil.getUser(updated.getUsername());
                if (linkedUser == null || !"Instructor".equalsIgnoreCase(linkedUser.getRole())) {
                    throw new IllegalArgumentException(
                            "Username (" + updated.getUsername() + ") must correspond to an instructor user.");
                }
                DatabaseUtil.updateFaculty(updated);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    loadFacultyData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FacultyPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    editButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void deleteFaculty() {
        if (blockIfMaintenance()) {
            return;
        }
        int selectedRow = facultyTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = facultyTable.convertRowIndexToModel(selectedRow);
        String facultyId = (String) tableModel.getValueAt(selectedRow, 0);
        String facultyName = (String) tableModel.getValueAt(selectedRow, 2);

        int option = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete faculty: " + facultyName + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            deleteButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseUtil.deleteFaculty(facultyId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        loadFacultyData();
                        JOptionPane.showMessageDialog(FacultyPanel.this, "Faculty deleted successfully!");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(FacultyPanel.this, "Error deleting: " + e.getMessage());
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
        boolean hasSelection = facultyTable.getSelectedRow() != -1;
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
}

package main.java.gui.panels;

import main.java.gui.dialogs.SectionDialog;
import main.java.models.Course;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;

import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel for managing course sections and schedule data.
 */
public class SectionPanel extends JPanel implements MaintenanceAware {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private JTable sectionTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton assignButton;
    private JButton refreshButton;
    private final User adminUser;
    private boolean maintenanceMode;

    private final String[] columnNames = {
            "Section ID",
            "Course",
            "Title",
            "Faculty",
            "Day",
            "Time",
            "Location",
            "Capacity",
            "Enrolled",
            "Waitlist",
            "Attendance %"
    };

    public SectionPanel(User adminUser) {
        this.adminUser = adminUser;
        initializeComponents();
        setupLayout();
        setupHandlers();
        loadData();
    }

    private void initializeComponents() {
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        sectionTable = new JTable(tableModel);
        sectionTable.setRowHeight(24);
        sectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sectionTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sectionTable.setRowSorter(sorter);

        searchField = new JTextField(20);
        searchField.setToolTipText("Search sections...");

        addButton = createButton("Add Section", new Color(34, 197, 94));
        editButton = createButton("Edit Section", new Color(37, 99, 235));
        deleteButton = createButton("Delete Section", new Color(220, 38, 38));
        assignButton = createButton("Assign Instructor", new Color(8, 145, 178));
        refreshButton = createButton("Refresh", new Color(107, 114, 128));

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        assignButton.setEnabled(false);
    }

    private JButton createButton(String text, Color color) {
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

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Section Scheduler");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(assignButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(refreshButton);

        JScrollPane tableScroll = new JScrollPane(sectionTable);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private void assignInstructor() {
        if (blockIfMaintenance())
            return;

        int viewRow = sectionTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }

        assignButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        int modelRow = sectionTable.convertRowIndexToModel(viewRow);
        String sectionId = (String) tableModel.getValueAt(modelRow, 0);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("section", DatabaseUtil.getSection(sectionId));
                result.put("faculties", DatabaseUtil.getAllFaculty().stream()
                        .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                        .collect(Collectors.toList()));
                return result;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    Section section = (Section) data.get("section");
                    List<Faculty> facultyList = (List<Faculty>) data.get("faculties");

                    if (section == null) {
                        JOptionPane.showMessageDialog(SectionPanel.this, "Unable to load section details.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (facultyList.isEmpty()) {
                        JOptionPane.showMessageDialog(SectionPanel.this, "No instructors available to assign.");
                        return;
                    }

                    showAssignDialog(section, facultyList);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SectionPanel.this, "Error preparing assignment: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    assignButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showAssignDialog(Section section, List<Faculty> facultyList) {
        Map<String, String> labelToId = new LinkedHashMap<>();
        JComboBox<String> combo = new JComboBox<>();
        for (Faculty faculty : facultyList) {
            String label = faculty.getFacultyId() + " - " + faculty.getFullName();
            combo.addItem(label);
            labelToId.put(label, faculty.getFacultyId());
            if (faculty.getFacultyId().equalsIgnoreCase(section.getFacultyId())) {
                combo.setSelectedItem(label);
            }
        }

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Assign instructor for " + section.getSectionId() + ":"), BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Assign Instructor",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION)
            return;

        String selectedLabel = (String) combo.getSelectedItem();
        if (selectedLabel == null)
            return;

        String facultyId = labelToId.get(selectedLabel);

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String actor = adminUser != null ? adminUser.getUsername() : "system";
                DatabaseUtil.assignInstructorToSection(section.getSectionId(), facultyId, actor);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(SectionPanel.this, "Instructor assigned successfully.");
                    loadData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SectionPanel.this, ex.getMessage(), "Assignment Failed",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void setupHandlers() {
        sectionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        searchField.addActionListener(e -> filterTable());

        addButton.addActionListener(e -> addSection());
        editButton.addActionListener(e -> editSection());
        deleteButton.addActionListener(e -> deleteSection());
        assignButton.addActionListener(e -> assignInstructor());
        refreshButton.addActionListener(e -> loadData());
    }

    private void loadData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        refreshButton.setEnabled(false);

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new java.util.ArrayList<>();
                Collection<Section> sections = DatabaseUtil.getAllSections();

                // Optimization: Bulk fetch courses and faculty if possible, or just accept loop
                // in background thread.
                // Since this is background, loop is acceptable for now.
                for (Section section : sections) {
                    Course course = DatabaseUtil.getCourse(section.getCourseId());
                    Faculty faculty = DatabaseUtil.getFaculty(section.getFacultyId());

                    String courseLabel = course != null
                            ? course.getCourseId() + " - " + course.getCourseName()
                            : section.getCourseId();
                    String facultyLabel = faculty != null
                            ? faculty.getFullName()
                            : section.getFacultyId();

                    String timeRange = section.getStartTime().format(TIME_FORMATTER) + " - "
                            + section.getEndTime().format(TIME_FORMATTER);

                    rows.add(new Object[] {
                            section.getSectionId(),
                            courseLabel,
                            section.getTitle(),
                            facultyLabel,
                            section.getDayOfWeek(),
                            timeRange,
                            section.getLocation(),
                            section.getCapacity(),
                            section.getEnrolledStudentIds().size(),
                            section.getWaitlistedStudentIds().size(),
                            String.format("%.0f%%", DatabaseUtil.getAverageAttendanceForSection(section.getSectionId()))
                    });
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SectionPanel.this, "Error loading sections: " + e.getMessage(),
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
        String text = searchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) sectionTable.getRowSorter();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void addSection() {
        if (blockIfMaintenance())
            return;

        SectionDialog dialog = new SectionDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Add Section",
                null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Section newSection = dialog.getSection();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseUtil.addSection(newSection);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(SectionPanel.this, "Section added successfully.");
                        loadData();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(SectionPanel.this, ex.getMessage(), "Unable to add section",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void editSection() {
        if (blockIfMaintenance())
            return;
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = sectionTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) tableModel.getValueAt(selectedRow, 0);

        // Quick fetch for dialog (could be async but usually fast enough for single
        // get)
        // Ideally should be async for strict correctness.
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Section, Void>() {
            @Override
            protected Section doInBackground() {
                return DatabaseUtil.getSection(sectionId);
            }

            @Override
            protected void done() {
                try {
                    Section section = get();
                    if (section == null) {
                        JOptionPane.showMessageDialog(SectionPanel.this, "Unable to locate section record.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    showEditDialog(section);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SectionPanel.this, "Error: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void showEditDialog(Section section) {
        SectionDialog dialog = new SectionDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Edit Section",
                section);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseUtil.updateSection(dialog.getSection());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(SectionPanel.this, "Section updated successfully.");
                        loadData();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(SectionPanel.this, ex.getMessage(), "Unable to update section",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }.execute();
        }
    }

    private void deleteSection() {
        if (blockIfMaintenance())
            return;

        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1)
            return;

        selectedRow = sectionTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) tableModel.getValueAt(selectedRow, 0);
        String sectionTitle = (String) tableModel.getValueAt(selectedRow, 2);

        int option = JOptionPane.showConfirmDialog(
                this,
                "Delete section \"" + sectionTitle + "\"? This will drop enrolments and attendance logs.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (option == JOptionPane.YES_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    DatabaseUtil.deleteSection(sectionId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(SectionPanel.this, "Section deleted.");
                        loadData();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(SectionPanel.this, "Error deleting: " + e.getMessage(), "Error",
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
        boolean hasSelection = sectionTable.getSelectedRow() != -1;
        boolean allowMutations = hasSelection && !maintenance;
        editButton.setEnabled(allowMutations);
        deleteButton.setEnabled(allowMutations);
        assignButton.setEnabled(allowMutations);
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

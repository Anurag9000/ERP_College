package main.java.gui.panels;

import main.java.gui.dialogs.SectionDialog;
import main.java.models.Course;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Panel for managing course sections and schedule data.
 */
public class SectionPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private JTable sectionTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;

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

    public SectionPanel() {
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
        refreshButton = createButton("Refresh", new Color(107, 114, 128));

        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
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
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(refreshButton);

        JScrollPane tableScroll = new JScrollPane(sectionTable);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private void setupHandlers() {
        sectionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selected = sectionTable.getSelectedRow() != -1;
                editButton.setEnabled(selected);
                deleteButton.setEnabled(selected);
            }
        });

        searchField.addActionListener(e -> filterTable());

        addButton.addActionListener(e -> addSection());
        editButton.addActionListener(e -> editSection());
        deleteButton.addActionListener(e -> deleteSection());
        refreshButton.addActionListener(e -> loadData());
    }

    private void loadData() {
        tableModel.setRowCount(0);

        Collection<Section> sections = DatabaseUtil.getAllSections();
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

            Object[] row = {
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
            };
            tableModel.addRow(row);
        }
    }

    private void filterTable() {
        String text = searchField.getText().trim();
        TableRowSorter<DefaultTableModel> sorter =
            (TableRowSorter<DefaultTableModel>) sectionTable.getRowSorter();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }
    }

    private void addSection() {
        SectionDialog dialog = new SectionDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Add Section",
                null
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            Section newSection = dialog.getSection();
            DatabaseUtil.addSection(newSection);
            loadData();
            JOptionPane.showMessageDialog(this, "Section added successfully.");
        }
    }

    private void editSection() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedRow = sectionTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) tableModel.getValueAt(selectedRow, 0);
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Unable to locate section record.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SectionDialog dialog = new SectionDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Edit Section",
                section
        );
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            DatabaseUtil.updateSection(dialog.getSection());
            loadData();
            JOptionPane.showMessageDialog(this, "Section updated successfully.");
        }
    }

    private void deleteSection() {
        int selectedRow = sectionTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        selectedRow = sectionTable.convertRowIndexToModel(selectedRow);
        String sectionId = (String) tableModel.getValueAt(selectedRow, 0);
        String sectionTitle = (String) tableModel.getValueAt(selectedRow, 2);

        int option = JOptionPane.showConfirmDialog(
                this,
                "Delete section \"" + sectionTitle + "\"? This will drop enrolments and attendance logs.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            DatabaseUtil.deleteSection(sectionId);
            loadData();
            JOptionPane.showMessageDialog(this, "Section deleted.");
        }
    }
}

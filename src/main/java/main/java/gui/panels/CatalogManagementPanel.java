package main.java.gui.panels;

import main.java.models.Course;
import main.java.models.Section;
import main.java.models.EnrollmentRecord;
import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/**
 * Admin catalog workspace for prerequisites, rooms, and capacity planning.
 */
public class CatalogManagementPanel extends JPanel implements ListSelectionListener {
    private final User adminUser;
    private final DefaultTableModel courseModel;
    private final JTable courseTable;
    private final JTextField searchField;
    private final DefaultListModel<String> prereqModel;
    private final DefaultListModel<String> coreqModel;
    private final DefaultListModel<String> antireqModel;
    private final JTextField prereqField;
    private final JTextField coreqField;
    private final JTextField antireqField;
    private final JButton saveRelationshipsButton;
    private final DefaultTableModel roomModel;
    private final DefaultTableModel warningModel;
    private Collection<Course> cachedCourses = List.of();

    public CatalogManagementPanel(User adminUser) {
        this.adminUser = adminUser;
        this.courseModel = new DefaultTableModel(new Object[]{
                "Code", "Name", "Department", "Credits", "Total Seats"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.courseTable = new JTable(courseModel);
        courseTable.setRowHeight(22);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.getSelectionModel().addListSelectionListener(this);

        this.searchField = new JTextField(20);
        this.prereqModel = new DefaultListModel<>();
        this.coreqModel = new DefaultListModel<>();
        this.antireqModel = new DefaultListModel<>();
        this.prereqField = new JTextField();
        this.coreqField = new JTextField();
        this.antireqField = new JTextField();

        this.saveRelationshipsButton = new JButton("Save Relationships");
        saveRelationshipsButton.setBackground(new Color(34, 197, 94));
        saveRelationshipsButton.setForeground(Color.WHITE);
        saveRelationshipsButton.setFocusPainted(false);

        this.roomModel = new DefaultTableModel(new Object[]{"Room", "Sections", "Total Capacity"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.warningModel = new DefaultTableModel(new Object[]{"Section", "Capacity", "Enrolled", "Over By"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        buildLayout();
        hookListeners();
        refreshData();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Catalog Management");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchPanel.add(new JLabel("Search Courses:"));
        searchPanel.add(searchField);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        searchPanel.add(refreshButton);
        header.add(searchPanel, BorderLayout.EAST);

        JScrollPane courseScroll = new JScrollPane(courseTable);
        courseScroll.setPreferredSize(new Dimension(450, 200));

        JPanel relationshipPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        relationshipPanel.add(buildListPanel("Prerequisites", prereqModel, prereqField,
                () -> addItem(prereqModel, prereqField),
                list -> removeSelected(prereqModel, list)));
        relationshipPanel.add(buildListPanel("Co-requisites", coreqModel, coreqField,
                () -> addItem(coreqModel, coreqField),
                list -> removeSelected(coreqModel, list)));
        relationshipPanel.add(buildListPanel("Anti-requisites", antireqModel, antireqField,
                () -> addItem(antireqModel, antireqField),
                list -> removeSelected(antireqModel, list)));

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        JScrollPane roomScroll = new JScrollPane(new JTable(roomModel));
        roomScroll.setBorder(BorderFactory.createTitledBorder("Room Utilization"));
        JScrollPane warningScroll = new JScrollPane(new JTable(warningModel));
        warningScroll.setBorder(BorderFactory.createTitledBorder("Capacity Warnings"));
        rightPanel.add(roomScroll, BorderLayout.CENTER);
        rightPanel.add(warningScroll, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(courseScroll, BorderLayout.CENTER);
        leftPanel.add(relationshipPanel, BorderLayout.SOUTH);
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.add(saveRelationshipsButton);
        leftPanel.add(savePanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.6);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildListPanel(String title,
                                  DefaultListModel<String> model,
                                  JTextField inputField,
                                  Runnable addAction,
                                  java.util.function.Consumer<JList<String>> removeAction) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JList<String> list = new JList<>(model);
        list.setVisibleRowCount(6);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(4, 4));
        inputField.setToolTipText("Course code (e.g., CSE101)");
        actions.add(inputField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton add = new JButton("Add");
        add.addActionListener(e -> addAction.run());
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> removeAction.accept(list));
        buttons.add(add);
        buttons.add(remove);
        actions.add(buttons, BorderLayout.SOUTH);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void hookListeners() {
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilter));
        saveRelationshipsButton.addActionListener(e -> saveRelationships());
    }

    private void refreshData() {
        cachedCourses = DatabaseUtil.getAllCourses().stream()
                .sorted(Comparator.comparing(Course::getCourseId))
                .collect(Collectors.toList());
        loadCourseTable();
        loadRoomStats();
        loadWarnings();
        if (!cachedCourses.isEmpty()) {
            courseTable.setRowSelectionInterval(0, 0);
        }
    }

    private void loadCourseTable() {
        courseModel.setRowCount(0);
        for (Course course : cachedCourses) {
            courseModel.addRow(new Object[]{
                    course.getCourseId(),
                    course.getCourseName(),
                    course.getDepartment(),
                    course.getCreditHours(),
                    course.getTotalSeats()
            });
        }
    }

    private void loadRoomStats() {
        roomModel.setRowCount(0);
        Map<String, List<Section>> byRoom = DatabaseUtil.getAllSections().stream()
                .filter(section -> section.getLocation() != null && !section.getLocation().isBlank())
                .collect(Collectors.groupingBy(section -> section.getLocation().trim(), Collectors.toList()));
        byRoom.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    int capacitySum = entry.getValue().stream().mapToInt(Section::getCapacity).sum();
                    roomModel.addRow(new Object[]{
                            entry.getKey(),
                            entry.getValue().size(),
                            capacitySum
                    });
                });
    }

    private void loadWarnings() {
        warningModel.setRowCount(0);
        DatabaseUtil.findCapacityWarnings().forEach(warning -> warningModel.addRow(new Object[]{
                warning.sectionId(),
                warning.capacity(),
                warning.enrolled(),
                warning.overBy()
        }));
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase(Locale.ENGLISH);
        courseModel.setRowCount(0);
        for (Course course : cachedCourses) {
            String haystack = (course.getCourseId() + " " + course.getCourseName() + " " + course.getDepartment())
                    .toLowerCase(Locale.ENGLISH);
            if (!query.isEmpty() && !haystack.contains(query)) {
                continue;
            }
            courseModel.addRow(new Object[]{
                    course.getCourseId(),
                    course.getCourseName(),
                    course.getDepartment(),
                    course.getCreditHours(),
                    course.getTotalSeats()
            });
        }
    }

    private void loadRelationshipsForSelectedCourse() {
        Course selected = getSelectedCourse();
        prereqModel.clear();
        coreqModel.clear();
        antireqModel.clear();
        if (selected == null) {
            return;
        }
        DatabaseUtil.getCoursePrerequisites(selected.getCourseId()).forEach(prereqModel::addElement);
        DatabaseUtil.getCourseCorequisites(selected.getCourseId()).forEach(coreqModel::addElement);
        DatabaseUtil.getCourseAntirequisites(selected.getCourseId()).forEach(antireqModel::addElement);
    }

    private Course getSelectedCourse() {
        int row = courseTable.getSelectedRow();
        if (row == -1) {
            return null;
        }
        row = courseTable.convertRowIndexToModel(row);
        String courseId = (String) courseModel.getValueAt(row, 0);
        return cachedCourses.stream()
                .filter(course -> Objects.equals(course.getCourseId(), courseId))
                .findFirst()
                .orElse(null);
    }

    private void addItem(DefaultListModel<String> model, JTextField field) {
        String value = field.getText().trim().toUpperCase(Locale.ENGLISH);
        if (value.isEmpty() || containsIgnoreCase(model, value)) {
            return;
        }
        model.addElement(value);
        field.setText("");
    }

    private boolean containsIgnoreCase(DefaultListModel<String> model, String value) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void removeSelected(DefaultListModel<String> model, JList<String> list) {
        int idx = list.getSelectedIndex();
        if (idx >= 0) {
            model.remove(idx);
        } else if (!model.isEmpty()) {
            model.remove(model.size() - 1);
        }
    }

    private List<String> toList(DefaultListModel<String> model) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            list.add(model.get(i));
        }
        return list;
    }

    private void saveRelationships() {
        Course course = getSelectedCourse();
        if (course == null) {
            JOptionPane.showMessageDialog(this, "Select a course first.");
            return;
        }
        if (DatabaseUtil.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        try {
            AdminService.updateCourseRelationships(adminUser,
                    course.getCourseId(),
                    toList(prereqModel),
                    toList(coreqModel),
                    toList(antireqModel));
            JOptionPane.showMessageDialog(this, "Relationships updated for " + course.getCourseId());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to save relationships", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            loadRelationshipsForSelectedCourse();
        }
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

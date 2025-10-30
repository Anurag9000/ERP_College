package main.java.gui.panels;

import main.java.models.AttendanceRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Panel for tracking attendance summaries by section.
 */
public class AttendancePanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private JComboBox<String> sectionCombo;
    private JTextField dateField;
    private JTable attendanceTable;
    private JTable historyTable;
    private DefaultTableModel attendanceModel;
    private DefaultTableModel historyModel;
    private JButton saveButton;
    private JButton markAllPresentButton;
    private JButton markAllAbsentButton;

    public AttendancePanel() {
        initializeComponents();
        setupLayout();
        setupHandlers();
        loadSections();
        refreshTables();
    }

    private void initializeComponents() {
        sectionCombo = new JComboBox<>();
        dateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 10);
        dateField.setToolTipText("Use format yyyy-MM-dd");

        attendanceModel = new DefaultTableModel(new Object[]{"Student ID", "Name", "Present"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        attendanceTable = new JTable(attendanceModel);
        attendanceTable.setRowHeight(24);

        historyModel = new DefaultTableModel(new Object[]{"Date", "Present %", "Notes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(22);

        saveButton = createButton("Save Attendance", new Color(34, 197, 94));
        markAllPresentButton = createButton("Mark All Present", new Color(37, 99, 235));
        markAllAbsentButton = createButton("Mark All Absent", new Color(220, 38, 38));
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

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Attendance Tracker");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filters.add(new JLabel("Section:"));
        filters.add(sectionCombo);
        filters.add(Box.createHorizontalStrut(10));
        filters.add(new JLabel("Date:"));
        filters.add(dateField);

        header.add(title, BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actions.add(markAllPresentButton);
        actions.add(markAllAbsentButton);
        actions.add(saveButton);

        JScrollPane attendanceScroll = new JScrollPane(attendanceTable);
        attendanceScroll.setBorder(BorderFactory.createTitledBorder("Mark Attendance"));

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Recent Sessions"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attendanceScroll, historyScroll);
        splitPane.setResizeWeight(0.6);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void setupHandlers() {
        sectionCombo.addActionListener(e -> refreshTables());
        saveButton.addActionListener(e -> saveAttendance());
        markAllPresentButton.addActionListener(e -> setAllAttendance(true));
        markAllAbsentButton.addActionListener(e -> setAllAttendance(false));
    }

    private void loadSections() {
        sectionCombo.removeAllItems();
        for (Section section : DatabaseUtil.getAllSections()) {
            sectionCombo.addItem(section.getSectionId() + " - " + section.getTitle());
        }
    }

    private void refreshTables() {
        attendanceModel.setRowCount(0);
        historyModel.setRowCount(0);

        if (sectionCombo.getSelectedItem() == null) {
            return;
        }

        String sectionId = ((String) sectionCombo.getSelectedItem()).split(" - ")[0];
        Section section = DatabaseUtil.getSection(sectionId);
        if (section == null) {
            return;
        }

        Map<String, Boolean> existingAttendance = loadSelectedDateAttendance(sectionId);
        for (String studentId : section.getEnrolledStudentIds()) {
            Student student = DatabaseUtil.getStudent(studentId);
            String name = student != null ? student.getFullName() : studentId;
            boolean present = existingAttendance.getOrDefault(studentId, Boolean.TRUE);
            attendanceModel.addRow(new Object[]{studentId, name, present});
        }

        List<AttendanceRecord> history = DatabaseUtil.getAttendanceForSection(sectionId);
        for (AttendanceRecord record : history) {
            historyModel.addRow(new Object[]{
                    record.getDate().format(DATE_FORMATTER),
                    String.format("%.0f%%", record.getAttendancePercentage()),
                    record.getAttendanceByStudent().size() + " responses"
            });
        }
    }

    private Map<String, Boolean> loadSelectedDateAttendance(String sectionId) {
        String dateText = dateField.getText().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(dateText, DATE_FORMATTER);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use yyyy-MM-dd.");
            dateField.setText(LocalDate.now().format(DATE_FORMATTER));
            date = LocalDate.now();
        }

        List<AttendanceRecord> history = DatabaseUtil.getAttendanceForSection(sectionId);
        final LocalDate targetDate = date;
        Optional<AttendanceRecord> record = history.stream()
                .filter(r -> r.getDate().equals(targetDate))
                .findFirst();

        return record.map(AttendanceRecord::getAttendanceByStudent).orElseGet(HashMap::new);
    }

    private void saveAttendance() {
        if (sectionCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FORMATTER);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use yyyy-MM-dd.");
            return;
        }

        String sectionId = ((String) sectionCombo.getSelectedItem()).split(" - ")[0];
        Map<String, Boolean> attendance = new HashMap<>();
        for (int i = 0; i < attendanceModel.getRowCount(); i++) {
            String studentId = (String) attendanceModel.getValueAt(i, 0);
            Boolean present = (Boolean) attendanceModel.getValueAt(i, 2);
            attendance.put(studentId, present != null && present);
        }

        DatabaseUtil.recordAttendance(sectionId, date, attendance);
        JOptionPane.showMessageDialog(this, "Attendance saved.");
        refreshTables();
    }

    private void setAllAttendance(boolean present) {
        for (int i = 0; i < attendanceModel.getRowCount(); i++) {
            attendanceModel.setValueAt(present, i, 2);
        }
    }
}

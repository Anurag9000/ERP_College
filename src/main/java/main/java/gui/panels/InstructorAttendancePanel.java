package main.java.gui.panels;

import main.java.models.Section;
import main.java.models.User;
import main.java.service.InstructorService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance capture tailored for instructors' assigned sections.
 */
public class InstructorAttendancePanel extends JPanel {
    private final User instructor;
    private final JComboBox<String> sectionCombo;
    private final JTextField dateField;
    private final DefaultTableModel tableModel;
    private java.util.List<Section> sections;

    public InstructorAttendancePanel(User instructor) {
        this.instructor = instructor;
        this.sectionCombo = new JComboBox<>();
        this.dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_DATE), 10);
        this.tableModel = new DefaultTableModel(new Object[]{"Student ID", "Present"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Section:"));
        top.add(sectionCombo);
        top.add(new JLabel("Date:"));
        top.add(dateField);
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> refreshRoster());
        top.add(loadButton);
        JButton saveButton = new JButton("Save" );
        saveButton.addActionListener(e -> saveAttendance());
        top.add(saveButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(tableModel)), BorderLayout.CENTER);

        refreshSections();
    }

    private void refreshSections() {
        sections = InstructorService.getAssignedSections(instructor);
        sectionCombo.removeAllItems();
        for (Section section : sections) {
            sectionCombo.addItem(section.getSectionId());
        }
        if (sectionCombo.getItemCount() > 0) {
            sectionCombo.setSelectedIndex(0);
        }
        refreshRoster();
    }

    private Section getSelectedSection() {
        int idx = sectionCombo.getSelectedIndex();
        if (idx < 0 || idx >= sections.size()) {
            return null;
        }
        return sections.get(idx);
    }

    private void refreshRoster() {
        tableModel.setRowCount(0);
        Section section = getSelectedSection();
        if (section == null) {
            return;
        }
        List<main.java.models.EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForSection(section.getSectionId());
        enrollments.stream()
                .filter(rec -> rec.getStatus() == main.java.models.EnrollmentRecord.Status.ENROLLED)
                .forEach(rec -> tableModel.addRow(new Object[]{rec.getStudentId(), Boolean.TRUE}));
    }

    private void saveAttendance() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format (yyyy-MM-dd).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String studentId = (String) tableModel.getValueAt(i, 0);
            Boolean present = (Boolean) tableModel.getValueAt(i, 1);
            map.put(studentId, present != null && present);
        }
        DatabaseUtil.recordAttendance(section.getSectionId(), date, map);
        JOptionPane.showMessageDialog(this, "Attendance saved.");
    }
}

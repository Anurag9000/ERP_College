package main.java.gui.dialogs;

import main.java.models.Course;
import main.java.models.Faculty;
import main.java.models.Section;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;

/**
 * Dialog for adding or editing a section.
 */
public class SectionDialog extends JDialog {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Section section;
    private boolean confirmed;

    private JTextField sectionIdField;
    private JTextField titleField;
    private JComboBox<String> courseCombo;
    private JComboBox<String> facultyCombo;
    private JComboBox<DayOfWeek> dayCombo;
    private JTextField startTimeField;
    private JTextField endTimeField;
    private JTextField locationField;
    private JSpinner capacitySpinner;

    public SectionDialog(JFrame parent, String title, Section section) {
        super(parent, title, true);
        this.section = section;
        this.confirmed = false;
        initializeComponents();
        setupLayout();
        setupHandlers();
        populateIfNeeded();

        setSize(420, 420);
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        sectionIdField = new JTextField(10);
        titleField = new JTextField(20);

        courseCombo = new JComboBox<>();
        Collection<Course> courses = DatabaseUtil.getAllCourses();
        for (Course course : courses) {
            courseCombo.addItem(course.getCourseId() + " - " + course.getCourseName());
        }

        facultyCombo = new JComboBox<>();
        Collection<Faculty> faculties = DatabaseUtil.getAllFaculty();
        for (Faculty faculty : faculties) {
            facultyCombo.addItem(faculty.getFacultyId() + " - " + faculty.getFullName());
        }

        dayCombo = new JComboBox<>(DayOfWeek.values());
        startTimeField = new JTextField("09:00", 5);
        endTimeField = new JTextField("10:30", 5);
        locationField = new JTextField(15);
        capacitySpinner = new JSpinner(new SpinnerNumberModel(30, 1, 200, 1));

        if (section == null) {
            String nextId = DatabaseUtil.generateNextId("SEC", DatabaseUtil.getAllSections());
            sectionIdField.setText(nextId);
            sectionIdField.setEditable(false);
        }
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Section ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sectionIdField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(titleField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Course:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(courseCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Faculty:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(facultyCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(dayCombo, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Start (HH:mm):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(startTimeField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("End (HH:mm):"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(endTimeField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(locationField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(capacitySpinner, gbc);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(107, 114, 128));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = new JButton("Save");
        saveButton.setBackground(new Color(34, 197, 94));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> onSave());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupHandlers() {
        // No-op placeholder for future validation hooks.
    }

    private void populateIfNeeded() {
        if (section == null) {
            return;
        }
        sectionIdField.setText(section.getSectionId());
        sectionIdField.setEditable(false);
        titleField.setText(section.getTitle());
        selectComboItem(courseCombo, section.getCourseId());
        selectComboItem(facultyCombo, section.getFacultyId());
        dayCombo.setSelectedItem(section.getDayOfWeek());
        startTimeField.setText(section.getStartTime().format(TIME_FORMATTER));
        endTimeField.setText(section.getEndTime().format(TIME_FORMATTER));
        locationField.setText(section.getLocation());
        capacitySpinner.setValue(section.getCapacity());
    }

    private void selectComboItem(JComboBox<String> combo, String id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(id)) {
                combo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSave() {
        if (courseCombo.getSelectedItem() == null || facultyCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Course and Faculty are required.");
            return;
        }

        LocalTime start;
        LocalTime end;

        try {
            start = LocalTime.parse(startTimeField.getText().trim(), TIME_FORMATTER);
            end = LocalTime.parse(endTimeField.getText().trim(), TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid time. Use HH:mm (e.g., 09:30).");
            return;
        }

        if (!end.isAfter(start)) {
            JOptionPane.showMessageDialog(this, "End time must be after start time.");
            return;
        }

        int capacity = (int) capacitySpinner.getValue();
        if (capacity <= 0) {
            JOptionPane.showMessageDialog(this, "Capacity must be greater than zero.");
            return;
        }

        String sectionId = sectionIdField.getText().trim();
        String title = titleField.getText().trim();
        String courseId = ((String) courseCombo.getSelectedItem()).split(" - ")[0];
        String facultyId = ((String) facultyCombo.getSelectedItem()).split(" - ")[0];
        DayOfWeek day = (DayOfWeek) dayCombo.getSelectedItem();
        String location = locationField.getText().trim();

        if (section == null) {
            section = new Section(sectionId, courseId, title, facultyId, day, start, end, location, capacity);
        } else {
            section.setTitle(title);
            section.setCourseId(courseId);
            section.setFacultyId(facultyId);
            section.setDayOfWeek(day);
            section.setStartTime(start);
            section.setEndTime(end);
            section.setLocation(location);
            section.setCapacity(capacity);
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Section getSection() {
        return section;
    }
}

package main.java.gui.dialogs;

import main.java.models.Faculty;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for faculty create/update operations.
 */
public class FacultyDialog extends JDialog {
    private final JTextField facultyIdField = new JTextField(15);
    private final JTextField usernameField = new JTextField(15);
    private final JTextField firstNameField = new JTextField(15);
    private final JTextField lastNameField = new JTextField(15);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField departmentField = new JTextField(15);
    private final JTextField designationField = new JTextField(15);
    private final JTextField qualificationField = new JTextField(15);
    private final JTextField salaryField = new JTextField(10);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "On Leave", "Resigned"});

    private boolean confirmed;
    private Faculty faculty;

    public FacultyDialog(JFrame parent, String title, Faculty faculty) {
        super(parent, title, true);
        this.faculty = faculty;
        setLayout(new BorderLayout());
        setBorder();
        buildForm();
        buildButtons();
        populate();
        pack();
        setLocationRelativeTo(parent);
    }

    private void setBorder() {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        addRow(form, gbc, row++, "Faculty ID", facultyIdField);
        addRow(form, gbc, row++, "Username", usernameField);
        addRow(form, gbc, row++, "First Name", firstNameField);
        addRow(form, gbc, row++, "Last Name", lastNameField);
        addRow(form, gbc, row++, "Email", emailField);
        addRow(form, gbc, row++, "Phone", phoneField);
        addRow(form, gbc, row++, "Department", departmentField);
        addRow(form, gbc, row++, "Designation", designationField);
        addRow(form, gbc, row++, "Qualification", qualificationField);
        addRow(form, gbc, row++, "Salary", salaryField);
        addRow(form, gbc, row++, "Status", statusCombo);
        add(form, BorderLayout.CENTER);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Save");
        save.addActionListener(e -> onSave());
        buttons.add(cancel);
        buttons.add(save);
        add(buttons, BorderLayout.SOUTH);
    }

    private void populate() {
        if (faculty == null) {
            facultyIdField.setText(DatabaseUtil.generateNextId("FAC", DatabaseUtil.getAllFaculty()));
            facultyIdField.setEditable(false);
            statusCombo.setSelectedItem("Active");
        } else {
            facultyIdField.setText(faculty.getFacultyId());
            facultyIdField.setEditable(false);
            usernameField.setText(faculty.getUsername());
            firstNameField.setText(faculty.getFirstName());
            lastNameField.setText(faculty.getLastName());
            emailField.setText(faculty.getEmail());
            phoneField.setText(faculty.getPhone());
            departmentField.setText(faculty.getDepartment());
            designationField.setText(faculty.getDesignation());
            qualificationField.setText(faculty.getQualification());
            salaryField.setText(String.valueOf(faculty.getSalary()));
            statusCombo.setSelectedItem(faculty.getStatus());
        }
    }

    private void onSave() {
        if (usernameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required");
            return;
        }
        double salary;
        try {
            salary = Double.parseDouble(salaryField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid salary");
            return;
        }
        if (faculty == null) {
            faculty = new Faculty();
            faculty.setFacultyId(facultyIdField.getText().trim());
        }
        faculty.setUsername(usernameField.getText().trim());
        faculty.setFirstName(firstNameField.getText().trim());
        faculty.setLastName(lastNameField.getText().trim());
        faculty.setEmail(emailField.getText().trim());
        faculty.setPhone(phoneField.getText().trim());
        faculty.setDepartment(departmentField.getText().trim());
        faculty.setDesignation(designationField.getText().trim());
        faculty.setQualification(qualificationField.getText().trim());
        faculty.setSalary(salary);
        faculty.setStatus((String) statusCombo.getSelectedItem());
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Faculty getFaculty() {
        return faculty;
    }
}

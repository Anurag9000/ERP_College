package main.java.gui.dialogs;

import main.java.models.Student;
import main.java.models.Course;
import main.java.utils.DatabaseUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;

/**
 * Dialog for adding/editing student information
 */
public class StudentDialog extends JDialog {
    private Student student;
    private boolean confirmed = false;
    
    private JTextField studentIdField;
    private JTextField usernameField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField dobField;
    private JTextArea addressArea;
    private JComboBox<String> courseCombo;
    private JSpinner semesterSpinner;
    private JComboBox<String> statusCombo;
    private JTextField totalFeesField;
    private JTextField feesPaidField;
    private JTextField cgpaField;
    private JTextField creditsCompletedField;
    private JTextField creditsInProgressField;
    private JTextField nextFeeDueField;
    private JTextField advisorField;
    private JComboBox<String> standingCombo;
    
    private JButton saveButton;
    private JButton cancelButton;
    
    public StudentDialog(JFrame parent, String title, Student student) {
        super(parent, title, true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.student = student;
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        if (student != null) {
            populateFields();
        }
        
        setSize(500, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    private void initializeComponents() {
        studentIdField = new JTextField(15);
        usernameField = new JTextField(15);
        firstNameField = new JTextField(15);
        lastNameField = new JTextField(15);
        emailField = new JTextField(15);
        phoneField = new JTextField(15);
        dobField = new JTextField(15);
        dobField.setToolTipText("Format: dd/MM/yyyy");
        
        addressArea = new JTextArea(3, 15);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        
        // Course combo
        courseCombo = new JComboBox<>();
        loadCourses();
        
        // Semester spinner
        semesterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        
        // Status combo
        statusCombo = new JComboBox<>(new String[]{"Active", "Inactive", "Graduated", "Suspended"});
        
        totalFeesField = new JTextField(15);
        feesPaidField = new JTextField(15);
        cgpaField = new JTextField(5);
        creditsCompletedField = new JTextField(5);
        creditsInProgressField = new JTextField(5);
        nextFeeDueField = new JTextField(10);
        nextFeeDueField.setToolTipText("Format: yyyy-MM-dd");
        advisorField = new JTextField(15);
        standingCombo = new JComboBox<>(new String[]{"Good", "Warning", "Probation"});
        
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        
        // Style buttons
        saveButton.setBackground(new Color(34, 197, 94));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        
        cancelButton.setBackground(new Color(107, 114, 128));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        
        // Auto-generate student ID if adding new student
        if (student == null) {
            String nextId = DatabaseUtil.generateNextId("STU", DatabaseUtil.getAllStudents());
            studentIdField.setText(nextId);
            studentIdField.setEditable(false);
        }
    }
    
    private void loadCourses() {
        Collection<Course> courses = DatabaseUtil.getAllCourses();
        courseCombo.removeAllItems();
        for (Course course : courses) {
            courseCombo.addItem(course.getCourseId() + " - " + course.getCourseName());
        }
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Student ID
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(studentIdField, gbc);
        row++;

        // Username
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(usernameField, gbc);
        row++;
        
        // First Name
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(firstNameField, gbc);
        row++;
        
        // Last Name
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(lastNameField, gbc);
        row++;
        
        // Email
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(emailField, gbc);
        row++;
        
        // Phone
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(phoneField, gbc);
        row++;
        
        // Date of Birth
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Date of Birth:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(dobField, gbc);
        row++;
        
        // Address
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JScrollPane(addressArea), gbc);
        row++;
        
        // Course
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Course:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(courseCombo, gbc);
        row++;
        
        // Semester
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Semester:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(semesterSpinner, gbc);
        row++;
        
        // Status
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(statusCombo, gbc);
        row++;
        
        // Total Fees
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Total Fees:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(totalFeesField, gbc);
        row++;
        
        // Fees Paid
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Fees Paid:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(feesPaidField, gbc);
        row++;

        // CGPA
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("CGPA:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(cgpaField, gbc);
        row++;

        // Credits Completed
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Credits Completed:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(creditsCompletedField, gbc);
        row++;

        // Credits In Progress
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Credits In Progress:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(creditsInProgressField, gbc);
        row++;

        // Next Fee Due
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Next Fee Due (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(nextFeeDueField, gbc);
        row++;

        // Advisor
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Advisor ID (optional):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(advisorField, gbc);
        row++;

        // Standing
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Academic Standing:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(standingCombo, gbc);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        saveButton.addActionListener(e -> saveStudent());
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
    }
    
    private void populateFields() {
        if (student != null) {
            studentIdField.setText(student.getStudentId());
            studentIdField.setEditable(false);

            if (student.getUsername() != null) {
                usernameField.setText(student.getUsername());
            }
            
            firstNameField.setText(student.getFirstName());
            lastNameField.setText(student.getLastName());
            emailField.setText(student.getEmail());
            phoneField.setText(student.getPhone());
            
            if (student.getDateOfBirth() != null) {
                dobField.setText(student.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            
            addressArea.setText(student.getAddress());
            
            // Set course combo
            for (int i = 0; i < courseCombo.getItemCount(); i++) {
                if (courseCombo.getItemAt(i).startsWith(student.getCourse())) {
                    courseCombo.setSelectedIndex(i);
                    break;
                }
            }
            
            semesterSpinner.setValue(student.getSemester());
            statusCombo.setSelectedItem(student.getStatus());
            totalFeesField.setText(String.valueOf(student.getTotalFees()));
            feesPaidField.setText(String.valueOf(student.getFeesPaid()));
            cgpaField.setText(String.format("%.2f", student.getCgpa()));
            creditsCompletedField.setText(String.valueOf(student.getCreditsCompleted()));
            creditsInProgressField.setText(String.valueOf(student.getCreditsInProgress()));
            if (student.getNextFeeDueDate() != null) {
                nextFeeDueField.setText(student.getNextFeeDueDate().toString());
            }
            if (student.getAdvisorId() != null) {
                advisorField.setText(student.getAdvisorId());
            }
            standingCombo.setSelectedItem(student.getAcademicStanding());
        }
    }
    
    private void saveStudent() {
        if (!validateFields()) {
            return;
        }
        
        try {
            String studentId = studentIdField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            
            LocalDate dateOfBirth = LocalDate.parse(dobField.getText().trim(), 
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            String address = addressArea.getText().trim();
            
            String courseSelection = (String) courseCombo.getSelectedItem();
            String courseId = courseSelection.split(" - ")[0];
            
            int semester = (Integer) semesterSpinner.getValue();
            String status = (String) statusCombo.getSelectedItem();
            
            double totalFees = Double.parseDouble(totalFeesField.getText().trim());
            double feesPaid = Double.parseDouble(feesPaidField.getText().trim());
            
            if (student == null) {
                // Creating new student
                student = new Student(studentId, firstName, lastName, email, phone, 
                                    dateOfBirth, address, courseId, semester);
            } else {
                // Updating existing student
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                student.setPhone(phone);
                student.setDateOfBirth(dateOfBirth);
                student.setAddress(address);
                student.setCourse(courseId);
                student.setSemester(semester);
            }
            
            double cgpa = Double.parseDouble(cgpaField.getText().trim().isEmpty() ? "0" : cgpaField.getText().trim());
            if (cgpa < 0 || cgpa > 10) {
                JOptionPane.showMessageDialog(this, "CGPA must be between 0 and 10.");
                return;
            }

            int creditsCompleted = Integer.parseInt(creditsCompletedField.getText().trim().isEmpty() ? "0" : creditsCompletedField.getText().trim());
            int creditsInProgress = Integer.parseInt(creditsInProgressField.getText().trim().isEmpty() ? "0" : creditsInProgressField.getText().trim());

            LocalDate nextDueDate = null;
            if (!nextFeeDueField.getText().trim().isEmpty()) {
                try {
                    nextDueDate = LocalDate.parse(nextFeeDueField.getText().trim());
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid next fee due date. Use yyyy-MM-dd.");
                    return;
                }
            }

            student.setStatus(status);
            student.setTotalFees(totalFees);
            student.setFeesPaid(feesPaid);
            student.setCgpa(cgpa);
            student.setCreditsCompleted(creditsCompleted);
            student.setCreditsInProgress(creditsInProgress);
            student.setNextFeeDueDate(nextDueDate);
            student.setAdvisorId(advisorField.getText().trim().isEmpty() ? null : advisorField.getText().trim());
            student.setAcademicStanding((String) standingCombo.getSelectedItem());
            student.setUsername(usernameField.getText().trim());
            
            confirmed = true;
            dispose();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error saving student: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean validateFields() {
        if (firstNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "First name is required");
            firstNameField.requestFocus();
            return false;
        }

        if (usernameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required");
            usernameField.requestFocus();
            return false;
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Last name is required");
            lastNameField.requestFocus();
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email is required");
            emailField.requestFocus();
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Phone is required");
            phoneField.requestFocus();
            return false;
        }
        
        // Validate date format
        try {
            LocalDate.parse(dobField.getText().trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use dd/MM/yyyy");
            dobField.requestFocus();
            return false;
        }
        
        // Validate fees
        try {
            double totalFees = Double.parseDouble(totalFeesField.getText().trim());
            double feesPaid = Double.parseDouble(feesPaidField.getText().trim());
            
            if (totalFees < 0 || feesPaid < 0) {
                JOptionPane.showMessageDialog(this, "Fees cannot be negative");
                return false;
            }
            
            if (feesPaid > totalFees) {
                JOptionPane.showMessageDialog(this, "Fees paid cannot exceed total fees");
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid fees format");
            return false;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public Student getStudent() {
        return student;
    }
}

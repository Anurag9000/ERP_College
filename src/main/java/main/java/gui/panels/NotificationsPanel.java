package main.java.gui.panels;

import main.java.models.NotificationMessage;
import main.java.models.NotificationRequest;
import main.java.models.Student;
import main.java.models.Faculty;
import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel to review and broadcast system notifications.
 */
public class NotificationsPanel extends JPanel implements MaintenanceAware {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final User adminUser;
    private JComboBox<String> audienceFilter;
    private JTable notificationTable;
    private DefaultTableModel tableModel;
    private JButton broadcastButton;
    private boolean maintenanceMode;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField categoryFilterField;
    private JButton applyFilterButton;
    private JButton resetFilterButton;
    private LocalDateTime filterFrom;
    private LocalDateTime filterTo;
    private String historyCategory;

    public NotificationsPanel() {
        this(null);
    }

    public NotificationsPanel(User adminUser) {
        this.adminUser = adminUser;
        initializeComponents();
        setupLayout();
        setupHandlers();
        loadNotifications();
    }

    private void initializeComponents() {
        audienceFilter = new JComboBox<>(new String[]{"All", "Students", "Instructors", "Admins"});
        broadcastButton = createButton("Broadcast Message", new Color(37, 99, 235));
        broadcastButton.setEnabled(adminUser != null);
        startDateField = new JTextField(10);
        startDateField.setToolTipText("YYYY-MM-DD");
        endDateField = new JTextField(10);
        endDateField.setToolTipText("YYYY-MM-DD");
        categoryFilterField = new JTextField(10);
        categoryFilterField.setToolTipText("Category contains");
        applyFilterButton = createButton("Apply", new Color(59, 130, 246));
        resetFilterButton = createButton("Reset", new Color(107, 114, 128));

        tableModel = new DefaultTableModel(new Object[]{"Time", "Audience", "Category", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        notificationTable = new JTable(tableModel);
        notificationTable.setRowHeight(24);
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
        JLabel title = new JLabel("Notification Center");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(new JLabel("Audience:"));
        controls.add(audienceFilter);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(broadcastButton);

        header.add(title, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterPanel.add(new JLabel("From:"));
        filterPanel.add(startDateField);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(endDateField);
        filterPanel.add(new JLabel("Category:"));
        filterPanel.add(categoryFilterField);
        filterPanel.add(applyFilterButton);
        filterPanel.add(resetFilterButton);

        JScrollPane tableScroll = new JScrollPane(notificationTable);

        JPanel north = new JPanel(new BorderLayout());
        north.add(header, BorderLayout.NORTH);
        north.add(filterPanel, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private void setupHandlers() {
        audienceFilter.addActionListener(e -> loadNotifications());
        broadcastButton.addActionListener(e -> broadcastMessage());
        applyFilterButton.addActionListener(e -> applyHistoryFilters());
        resetFilterButton.addActionListener(e -> resetHistoryFilters());
    }

    private void loadNotifications() {
        tableModel.setRowCount(0);

        NotificationMessage.Audience audience = null;
        String selection = (String) audienceFilter.getSelectedItem();
        if ("Students".equalsIgnoreCase(selection)) {
            audience = NotificationMessage.Audience.STUDENT;
        } else if ("Instructors".equalsIgnoreCase(selection)) {
            audience = NotificationMessage.Audience.INSTRUCTOR;
        } else if ("Admins".equalsIgnoreCase(selection)) {
            audience = NotificationMessage.Audience.ADMIN;
        }

        List<NotificationMessage> notifications = DatabaseUtil.getNotificationsForAdmin(audience, filterFrom, filterTo, historyCategory);
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        for (NotificationMessage message : notifications) {
            tableModel.addRow(new Object[]{
                    message.getCreatedAt().format(FORMATTER),
                    message.getAudience().name(),
                    message.getCategory(),
                    message.getMessage()
            });
        }
    }

    private void applyHistoryFilters() {
        try {
            filterFrom = parseDate(startDateField.getText(), true);
            filterTo = parseDate(endDateField.getText(), false);
            String category = categoryFilterField.getText().trim();
            historyCategory = category.isEmpty() ? null : category;
            loadNotifications();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Dates must follow yyyy-MM-dd format.",
                    "Invalid date",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetHistoryFilters() {
        startDateField.setText("");
        endDateField.setText("");
        categoryFilterField.setText("");
        filterFrom = null;
        filterTo = null;
        historyCategory = null;
        loadNotifications();
    }

    private LocalDateTime parseDate(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDate date = LocalDate.parse(value.trim(), DATE_INPUT);
        return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
    }

    private void broadcastMessage() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        if (adminUser == null) {
            JOptionPane.showMessageDialog(this, "This panel must be initialised with an admin user to broadcast.");
            return;
        }
        JComboBox<TargetOption> targetSelector = new JComboBox<>(TargetOption.values());
        JComboBox<String> departmentSelector = new JComboBox<>(buildDepartmentModel());
        departmentSelector.insertItemAt("-- Select Department --", 0);
        departmentSelector.setSelectedIndex(0);
        JTextField userField = new JTextField();
        JComboBox<String> categorySelector = new JComboBox<>(new String[]{
                "Maintenance",
                "System",
                "Finance",
                "Registrar",
                "Academic",
                "General"
        });
        categorySelector.setEditable(true);
        categorySelector.setSelectedItem("General");
        JCheckBox emailChannel = new JCheckBox("Email stub");
        JCheckBox smsChannel = new JCheckBox("SMS stub");
        JTextArea messageArea = new JTextArea(4, 25);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Target:"), gbc);
        gbc.gridx = 1;
        form.add(targetSelector, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Department:"), gbc);
        gbc.gridx = 1;
        form.add(departmentSelector, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Username/ID:"), gbc);
        gbc.gridx = 1;
        form.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        form.add(categorySelector, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        form.add(emailChannel, gbc);
        gbc.gridx = 1;
        form.add(smsChannel, gbc);

        targetSelector.addActionListener(e -> toggleTargetInputs((TargetOption) targetSelector.getSelectedItem(), departmentSelector, userField));
        toggleTargetInputs((TargetOption) targetSelector.getSelectedItem(), departmentSelector, userField);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Broadcast Notification",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String messageText = messageArea.getText().trim();
        if (messageText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Message cannot be empty.");
            return;
        }
        TargetOption option = (TargetOption) targetSelector.getSelectedItem();
        String targetValue = resolveTargetValue(option, departmentSelector, userField);
        String category = categorySelector.getSelectedItem() != null
                ? categorySelector.getSelectedItem().toString().trim() : "";
        if (category.isEmpty()) {
            category = "General";
        }
        try {
            NotificationRequest request = new NotificationRequest(
                    option.getType(),
                    targetValue,
                    category,
                    messageText,
                    emailChannel.isSelected(),
                    smsChannel.isSelected());
            AdminService.broadcastNotification(adminUser, request);
            JOptionPane.showMessageDialog(this, "Notification broadcasted.");
            loadNotifications();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to broadcast", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        broadcastButton.setEnabled(!maintenance && adminUser != null);
    }

    private void toggleTargetInputs(TargetOption option,
                                    JComboBox<String> departmentSelector,
                                    JTextField userField) {
        boolean needsDepartment = option != null && option.requiresDepartment();
        boolean needsUser = option != null && option.requiresUser();
        departmentSelector.setEnabled(needsDepartment);
        userField.setEnabled(needsUser);
    }

    private String resolveTargetValue(TargetOption option,
                                      JComboBox<String> departmentSelector,
                                      JTextField userField) {
        if (option == null) {
            return null;
        }
        if (option.requiresDepartment()) {
            Object selected = departmentSelector.getSelectedItem();
            if (selected == null || selected.toString().startsWith("--")) {
                throw new IllegalArgumentException("Select a department.");
            }
            return selected.toString();
        }
        if (option.requiresUser()) {
            String username = userField.getText().trim();
            if (username.isEmpty()) {
                throw new IllegalArgumentException("Enter a username or ID.");
            }
            return username;
        }
        return null;
    }

    private DefaultComboBoxModel<String> buildDepartmentModel() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String department : availableDepartments()) {
            model.addElement(department);
        }
        return model;
    }

    private List<String> availableDepartments() {
        Set<String> departments = new LinkedHashSet<>();
        DatabaseUtil.getAllStudents().forEach(student -> {
            if (student.getDepartment() != null && !student.getDepartment().isBlank()) {
                departments.add(student.getDepartment().trim());
            }
        });
        DatabaseUtil.getAllFaculty().forEach(faculty -> {
            if (faculty.getDepartment() != null && !faculty.getDepartment().isBlank()) {
                departments.add(faculty.getDepartment().trim());
            }
        });
        return departments.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private enum TargetOption {
        ALL("All Users", NotificationRequest.TargetType.ALL, false, false),
        STUDENTS("All Students", NotificationRequest.TargetType.STUDENTS, false, false),
        INSTRUCTORS("All Instructors", NotificationRequest.TargetType.INSTRUCTORS, false, false),
        ADMINS("All Admins", NotificationRequest.TargetType.ADMINS, false, false),
        USER("Specific User", NotificationRequest.TargetType.USER, true, false),
        STUDENT_DEPARTMENT("Students by Department", NotificationRequest.TargetType.STUDENT_DEPARTMENT, false, true),
        INSTRUCTOR_DEPARTMENT("Instructors by Department", NotificationRequest.TargetType.INSTRUCTOR_DEPARTMENT, false, true);

        private final String label;
        private final NotificationRequest.TargetType type;
        private final boolean requiresUser;
        private final boolean requiresDepartment;

        TargetOption(String label, NotificationRequest.TargetType type, boolean requiresUser, boolean requiresDepartment) {
            this.label = label;
            this.type = type;
            this.requiresUser = requiresUser;
            this.requiresDepartment = requiresDepartment;
        }

        public NotificationRequest.TargetType getType() {
            return type;
        }

        public boolean requiresUser() {
            return requiresUser;
        }

        public boolean requiresDepartment() {
            return requiresDepartment;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

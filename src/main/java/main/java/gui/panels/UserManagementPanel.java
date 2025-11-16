package main.java.gui.panels;

import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;
import main.java.utils.AuditLogService;
import main.java.gui.panels.MaintenanceAware;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Administrative user provisioning and password reset panel.
 */
public class UserManagementPanel extends JPanel implements MaintenanceAware {
    private final User adminUser;
    private final DefaultTableModel tableModel;
    private final JTable userTable;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton resetPasswordButton;
    private final JButton toggleStatusButton;
    private final JButton auditButton;
    private boolean maintenanceMode;

    public UserManagementPanel(User adminUser) {
        this.adminUser = adminUser;
        this.tableModel = new DefaultTableModel(new Object[]{
                "Username", "Role", "Full Name", "Email", "Active", "Last Login"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.userTable = new JTable(tableModel);
        userTable.setRowHeight(22);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.addButton = new JButton("Add User");
        this.editButton = new JButton("Edit");
        this.resetPasswordButton = new JButton("Reset Password");
        this.toggleStatusButton = new JButton("Suspend/Activate");
        this.auditButton = new JButton("View Audit");
        top.add(addButton);
        top.add(editButton);
        top.add(resetPasswordButton);
        top.add(toggleStatusButton);
        top.add(auditButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        addButton.addActionListener(e -> addUser());
        editButton.addActionListener(e -> editUser());
        resetPasswordButton.addActionListener(e -> resetPassword());
        toggleStatusButton.addActionListener(e -> toggleUserStatus());
        auditButton.addActionListener(e -> viewAuditTrail());

        refresh();
    }

    private void refresh() {
        tableModel.setRowCount(0);
        for (User user : DatabaseUtil.getAllUsers()) {
            tableModel.addRow(new Object[]{
                    user.getUsername(),
                    user.getRole(),
                    user.getFullName(),
                    user.getEmail(),
                    user.isActive(),
                    user.getLastLogin() == null ? "—" : user.getLastLogin().toString()
            });
        }
    }

    private void addUser() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        JTextField usernameField = new JTextField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Student", "Instructor", "Admin"});
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField tempPasswordField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Role"));
        panel.add(roleCombo);
        panel.add(new JLabel("Full Name"));
        panel.add(nameField);
        panel.add(new JLabel("Email"));
        panel.add(emailField);
        panel.add(new JLabel("Temp Password"));
        panel.add(tempPasswordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            AdminService.createUser(adminUser,
                    usernameField.getText().trim(),
                    (String) roleCombo.getSelectedItem(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    tempPasswordField.getText().trim());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to add user", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editUser() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        JTextField nameField = new JTextField(user.getFullName());
        JTextField emailField = new JTextField(user.getEmail());
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Student", "Instructor", "Admin"});
        roleCombo.setSelectedItem(user.getRole());

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Full Name"));
        panel.add(nameField);
        panel.add(new JLabel("Email"));
        panel.add(emailField);
        panel.add(new JLabel("Role"));
        panel.add(roleCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            AdminService.updateUserProfile(adminUser, user.getUsername(),
                    nameField.getText().trim(), emailField.getText().trim());
            String newRole = (String) roleCombo.getSelectedItem();
            if (newRole != null && !newRole.equalsIgnoreCase(user.getRole())) {
                AdminService.updateUserRole(adminUser, user.getUsername(), newRole);
            }
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update user", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetPassword() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        String username = (String) tableModel.getValueAt(row, 0);
        String newPassword = JOptionPane.showInputDialog(this, "Enter new temporary password for " + username + ":");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return;
        }
        try {
            DatabaseUtil.resetPasswordByAdmin(username, newPassword.trim());
            JOptionPane.showMessageDialog(this, "Password updated. User must change on next login.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to reset password", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleUserStatus() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        String username = (String) tableModel.getValueAt(row, 0);
        boolean isActive = (Boolean) tableModel.getValueAt(row, 4);
        int result = JOptionPane.showConfirmDialog(this,
                (isActive ? "Suspend " : "Reactivate ") + username + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            AdminService.setUserActive(adminUser, username, !isActive);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update status", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewAuditTrail() {
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        List<AuditLogService.AuditEvent> events = AdminService.auditTrailForUser(user.getUsername());
        JTextArea textArea = new JTextArea(AuditLogService.toDisplayString(events));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        JOptionPane.showMessageDialog(this, scrollPane,
                "Audit Trail for " + user.getUsername(), JOptionPane.INFORMATION_MESSAGE);
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            return null;
        }
        String username = (String) tableModel.getValueAt(row, 0);
        return DatabaseUtil.getUser(username);
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        addButton.setEnabled(!maintenance);
        editButton.setEnabled(!maintenance);
        resetPasswordButton.setEnabled(!maintenance);
        toggleStatusButton.setEnabled(!maintenance);
        auditButton.setEnabled(true);
    }
}

package main.java.gui.panels;

import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Administrative user provisioning and password reset panel.
 */
public class UserManagementPanel extends JPanel {
    private final User adminUser;
    private final DefaultTableModel tableModel;
    private final JTable userTable;

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
        JButton addButton = new JButton("Add User");
        JButton resetPasswordButton = new JButton("Reset Password");
        top.add(addButton);
        top.add(resetPasswordButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(userTable), BorderLayout.CENTER);

        addButton.addActionListener(e -> addUser());
        resetPasswordButton.addActionListener(e -> resetPassword());

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

    private void resetPassword() {
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
            DatabaseUtil.changePassword(username, newPassword.trim());
            JOptionPane.showMessageDialog(this, "Password updated.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to reset password", JOptionPane.ERROR_MESSAGE);
        }
    }
}

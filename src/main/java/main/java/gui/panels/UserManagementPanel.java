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
        this.tableModel = new DefaultTableModel(new Object[] {
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
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<User>, Void>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return DatabaseUtil.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<User> users = get();
                    tableModel.setRowCount(0);
                    for (User user : users) {
                        tableModel.addRow(new Object[] {
                                user.getUsername(),
                                user.getRole(),
                                user.getFullName(),
                                user.getEmail(),
                                user.isActive(),
                                user.getLastLogin() == null ? "—" : user.getLastLogin().toString()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "Error loading users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void addUser() {
        if (blockIfMaintenance()) {
            return;
        }
        JTextField usernameField = new JTextField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[] { "Student", "Instructor", "Admin" });
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

        int result = JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        addButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        String username = usernameField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = tempPasswordField.getText().trim();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AdminService.createUser(adminUser, username, role, name, email, password);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, ex.getMessage(), "Unable to add user",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    addButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void editUser() {
        if (blockIfMaintenance()) {
            return;
        }
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        JTextField nameField = new JTextField(user.getFullName());
        JTextField emailField = new JTextField(user.getEmail());
        JComboBox<String> roleCombo = new JComboBox<>(new String[] { "Student", "Instructor", "Admin" });
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

        editButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        String username = user.getUsername();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String newRole = (String) roleCombo.getSelectedItem();
        String currentRole = user.getRole();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AdminService.updateUserProfile(adminUser, username, name, email);
                if (newRole != null && !newRole.equalsIgnoreCase(currentRole)) {
                    AdminService.updateUserRole(adminUser, username, newRole);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, ex.getMessage(), "Unable to update user",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    editButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void resetPassword() {
        if (blockIfMaintenance()) {
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

        resetPasswordButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String finalPassword = newPassword.trim();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatabaseUtil.resetPasswordByAdmin(username, finalPassword);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(UserManagementPanel.this,
                            "Password updated. User must change on next login.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, ex.getMessage(), "Unable to reset password",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    resetPasswordButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void toggleUserStatus() {
        if (blockIfMaintenance()) {
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

        toggleStatusButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AdminService.setUserActive(adminUser, username, !isActive);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, ex.getMessage(), "Unable to update status",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    toggleStatusButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void viewAuditTrail() {
        User user = getSelectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }

        auditButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String username = user.getUsername();

        new SwingWorker<List<AuditLogService.AuditEvent>, Void>() {
            @Override
            protected List<AuditLogService.AuditEvent> doInBackground() throws Exception {
                return AdminService.auditTrailForUser(username);
            }

            @Override
            protected void done() {
                try {
                    List<AuditLogService.AuditEvent> events = get();
                    JTextArea textArea = new JTextArea(AuditLogService.toDisplayString(events));
                    textArea.setEditable(false);
                    textArea.setCaretPosition(0);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(600, 300));
                    JOptionPane.showMessageDialog(UserManagementPanel.this, scrollPane,
                            "Audit Trail for " + username, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "Error loading audit: " + ex.getMessage());
                } finally {
                    auditButton.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
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
        applyButtonStates();
        auditButton.setEnabled(true);
    }

    private void applyButtonStates() {
        boolean maintenance = isMaintenanceLocked();
        addButton.setEnabled(!maintenance);
        editButton.setEnabled(!maintenance);
        resetPasswordButton.setEnabled(!maintenance);
        toggleStatusButton.setEnabled(!maintenance);
    }

    private boolean blockIfMaintenance() {
        if (isMaintenanceLocked()) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return true;
        }
        return false;
    }

    private boolean isMaintenanceLocked() {
        boolean maintenance = maintenanceMode || DatabaseUtil.isMaintenanceMode();
        if (maintenanceMode != maintenance) {
            maintenanceMode = maintenance;
        }
        return maintenance;
    }
}

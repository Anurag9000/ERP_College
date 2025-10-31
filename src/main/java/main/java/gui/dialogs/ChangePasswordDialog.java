package main.java.gui.dialogs;

import main.java.utils.DatabaseUtil;
import main.java.utils.PasswordPolicy;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog to handle user-initiated password change.
 */
public class ChangePasswordDialog extends JDialog {
    private final String username;
    private boolean changed;

    private final JPasswordField currentPassword = new JPasswordField(20);
    private final JPasswordField newPassword = new JPasswordField(20);
    private final JPasswordField confirmPassword = new JPasswordField(20);

    public ChangePasswordDialog(JFrame parent, String username) {
        super(parent, "Change Password", true);
        this.username = username;
        buildUI();
        setResizable(false);
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, gbc, row++, "Current Password", currentPassword);
        addRow(form, gbc, row++, "New Password", newPassword);
        addRow(form, gbc, row++, "Confirm Password", confirmPassword);

        JLabel hint = new JLabel("Password must include upper, lower, digit, special, min 10 chars.");
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        form.add(hint, gbc);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Change");
        save.addActionListener(e -> onChange());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private void onChange() {
        char[] current = currentPassword.getPassword();
        char[] next = newPassword.getPassword();
        char[] confirm = confirmPassword.getPassword();

        if (next.length == 0 || confirm.length == 0) {
            JOptionPane.showMessageDialog(this, "Enter the new password.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!java.util.Arrays.equals(next, confirm)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            PasswordPolicy.validateComplexity(new String(next));
            DatabaseUtil.changePasswordSelf(username, new String(current), new String(next));
            changed = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Password Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            java.util.Arrays.fill(current, '\0');
            java.util.Arrays.fill(next, '\0');
            java.util.Arrays.fill(confirm, '\0');
        }
    }

    public boolean isChanged() {
        return changed;
    }
}

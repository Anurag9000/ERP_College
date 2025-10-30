package main.java.gui.panels;

import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
/**
 * Admin control surface for maintenance toggle and quick status.
 */
public class MaintenancePanel extends JPanel {
    private final User adminUser;
    private final Runnable onToggleCallback;
    private final JToggleButton toggleButton;
    private final JLabel statusLabel;

    public MaintenancePanel(User adminUser, Runnable onToggleCallback) {
        this.adminUser = adminUser;
        this.onToggleCallback = onToggleCallback;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Maintenance Mode");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        toggleButton = new JToggleButton();
        toggleButton.setPreferredSize(new Dimension(160, 40));
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        toggleButton.addActionListener(e -> handleToggle());

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        center.add(toggleButton);
        center.add(statusLabel);

        JTextArea infoArea = new JTextArea(
                "When maintenance is ON, students and instructors can view data but cannot make changes.\n" +
                "Use this during deployments or data fixes. Notifications are broadcast automatically."
        );
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());
        infoArea.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        add(center, BorderLayout.CENTER);
        add(infoArea, BorderLayout.SOUTH);

        refreshState();
    }

    private void refreshState() {
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        toggleButton.setText(maintenance ? "Switch OFF" : "Switch ON");
        toggleButton.setSelected(maintenance);
        toggleButton.setForeground(maintenance ? Color.RED.darker() : new Color(34, 197, 94));
        statusLabel.setText("Current status: " + (maintenance ? "ON" : "OFF"));
    }

    private void handleToggle() {
        boolean desired = !DatabaseUtil.isMaintenanceMode();
        try {
            AdminService.toggleMaintenance(adminUser, desired);
            refreshState();
        if (onToggleCallback != null) {
            onToggleCallback.run();
        }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to toggle", JOptionPane.ERROR_MESSAGE);
        }
    }
}

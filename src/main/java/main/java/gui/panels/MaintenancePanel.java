package main.java.gui.panels;

import main.java.models.MaintenanceWindow;
import main.java.models.User;
import main.java.service.AdminService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Admin control surface for maintenance scheduling and quick status.
 */
public class MaintenancePanel extends JPanel {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a");
    private final User adminUser;
    private final Runnable onToggleCallback;
    private final JToggleButton toggleButton;
    private final JLabel statusLabel;
    private final JLabel nextWindowLabel;
    private final JLabel countdownLabel;
    private final DefaultTableModel scheduleModel;
    private final JTable scheduleTable;
    private final JButton cancelWindowButton;
    private final JButton scheduleButton;
    private final JButton refreshButton;
    private final JSpinner startSpinner;
    private final JSpinner durationSpinner;
    private final JTextField messageField;
    private Timer countdownTimer;
    private MaintenanceWindow nextWindow;

    public MaintenancePanel(User adminUser, Runnable onToggleCallback) {
        this.adminUser = adminUser;
        this.onToggleCallback = onToggleCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        toggleButton = new JToggleButton();
        toggleButton.setPreferredSize(new Dimension(160, 40));
        toggleButton.setFont(new Font("Arial", Font.BOLD, 14));
        toggleButton.addActionListener(e -> handleToggle());

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        nextWindowLabel = new JLabel("No upcoming window scheduled.");
        nextWindowLabel.setFont(new Font("Arial", Font.BOLD, 14));

        countdownLabel = new JLabel("");
        countdownLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        countdownLabel.setForeground(new Color(107, 114, 128));

        scheduleModel = new DefaultTableModel(new Object[]{"ID", "Start", "End", "Status", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        scheduleTable = new JTable(scheduleModel);
        scheduleTable.setRowHeight(22);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        cancelWindowButton = new JButton("Cancel Window");
        cancelWindowButton.addActionListener(e -> handleCancel());

        scheduleButton = new JButton("Schedule Window");
        scheduleButton.addActionListener(e -> handleSchedule());

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshSchedule());

        startSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE));
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd MMM yyyy HH:mm"));
        durationSpinner = new JSpinner(new SpinnerNumberModel(60, 15, 720, 15));
        messageField = new JTextField();

        buildLayout();
        refreshState();
        refreshSchedule();
        startCountdownTimer();
    }

    private void buildLayout() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Maintenance Control Center");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel togglePanel = new JPanel();
        togglePanel.setLayout(new BoxLayout(togglePanel, BoxLayout.Y_AXIS));
        togglePanel.add(toggleButton);
        togglePanel.add(Box.createVerticalStrut(5));
        togglePanel.add(statusLabel);

        header.add(togglePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Upcoming Window"));
        infoPanel.add(nextWindowLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(countdownLabel);

        JPanel schedulerPanel = new JPanel();
        schedulerPanel.setLayout(new GridBagLayout());
        schedulerPanel.setBorder(BorderFactory.createTitledBorder("Plan Maintenance Window"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        schedulerPanel.add(new JLabel("Start:"), gbc);
        gbc.gridx = 1;
        schedulerPanel.add(startSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        schedulerPanel.add(new JLabel("Duration (minutes):"), gbc);
        gbc.gridx = 1;
        schedulerPanel.add(durationSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        schedulerPanel.add(new JLabel("Message:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        schedulerPanel.add(messageField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        schedulerPanel.add(scheduleButton, gbc);

        JPanel leftColumn = new JPanel(new BorderLayout(10, 10));
        leftColumn.add(infoPanel, BorderLayout.NORTH);
        leftColumn.add(schedulerPanel, BorderLayout.CENTER);

        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableActions.add(refreshButton);
        tableActions.add(cancelWindowButton);

        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Scheduled Windows"));
        tablePanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);
        tablePanel.add(tableActions, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.add(leftColumn, BorderLayout.WEST);
        center.add(tablePanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JTextArea infoArea = new JTextArea(
                "When maintenance is ON, students and instructors can view data but cannot make changes.\n" +
                        "Schedule windows ahead of time to broadcast countdown banners automatically."
        );
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setEditable(false);
        infoArea.setBackground(getBackground());
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        add(infoArea, BorderLayout.SOUTH);
    }

    private void refreshState() {
        boolean maintenance = DatabaseUtil.isMaintenanceMode();
        toggleButton.setText(maintenance ? "Switch OFF" : "Switch ON");
        toggleButton.setSelected(maintenance);
        toggleButton.setForeground(maintenance ? Color.RED.darker() : new Color(34, 197, 94));
        statusLabel.setText("Current status: " + (maintenance ? "ON" : "OFF"));
    }

    private void refreshSchedule() {
        try {
            List<MaintenanceWindow> windows = AdminService.getMaintenanceWindows(adminUser);
            scheduleModel.setRowCount(0);
            for (MaintenanceWindow window : windows) {
                scheduleModel.addRow(new Object[]{
                        window.getId(),
                        DISPLAY_FORMAT.format(window.getStartAt()),
                        DISPLAY_FORMAT.format(window.getEndAt()),
                        window.getStatus().name(),
                        window.getMessage()
                });
            }
            Optional<MaintenanceWindow> upcoming = AdminService.getNextMaintenanceWindow(adminUser);
            nextWindow = upcoming.orElse(null);
            updateNextWindowLabel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Unable to load schedule", JOptionPane.ERROR_MESSAGE);
        }
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

    private void handleSchedule() {
        try {
            Date startDate = (Date) startSpinner.getValue();
            int durationMinutes = ((Number) durationSpinner.getValue()).intValue();
            if (durationMinutes < 15) {
                throw new IllegalArgumentException("Duration must be at least 15 minutes.");
            }
            LocalDateTime start = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
            LocalDateTime end = start.plusMinutes(durationMinutes);
            AdminService.scheduleMaintenanceWindow(adminUser, start, end, messageField.getText());
            messageField.setText("");
            refreshSchedule();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Unable to schedule maintenance", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancel() {
        int row = scheduleTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a window to cancel.",
                    "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long windowId = Long.parseLong(scheduleModel.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel maintenance window #" + windowId + "?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            AdminService.cancelMaintenanceWindow(adminUser, windowId);
            refreshSchedule();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Unable to cancel window", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNextWindowLabel() {
        if (nextWindow == null) {
            nextWindowLabel.setText("No upcoming window scheduled.");
            countdownLabel.setText("");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean active = nextWindow.isActive(now);
        if (active) {
            nextWindowLabel.setText("Active now · ends " + DISPLAY_FORMAT.format(nextWindow.getEndAt()));
            Duration remaining = Duration.between(now, nextWindow.getEndAt());
            countdownLabel.setText("Ends in " + humanize(remaining));
        } else {
            nextWindowLabel.setText("Starts " + DISPLAY_FORMAT.format(nextWindow.getStartAt()));
            Duration until = Duration.between(now, nextWindow.getStartAt());
            countdownLabel.setText("Starts in " + humanize(until));
        }
    }

    private void startCountdownTimer() {
        countdownTimer = new Timer(1_000, e -> updateNextWindowLabel());
        countdownTimer.start();
    }

    private String humanize(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return "less than a minute";
        }
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return minutes + "m";
    }
}

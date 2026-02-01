package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.NotificationPreference;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Notification preferences panel with per-category controls
 */
public class NotificationPreferencesPanel extends JPanel {

    private final User currentUser;
    private Map<String, JCheckBox> categoryCheckboxes;
    private JComboBox<String> digestFrequencyCombo;

    public NotificationPreferencesPanel(User currentUser) {
        this.currentUser = currentUser;
        this.categoryCheckboxes = new HashMap<>();

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Notification Preferences");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        mainPanel.add(createCategoryPreferencesCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createDigestSettingsCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createSaveButtonPanel());

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        loadPreferences();
    }

    private void loadPreferences() {
        try {
            String userId = resolveUserId();
            NotificationPreference pref = DatabaseUtil.getNotificationPreference(userId);

            // Map models to UI
            emailEnabledCheckbox.setSelected(pref.isEmailEnabled());
            smsEnabledCheckbox.setSelected(pref.isSmsEnabled());

            switch (pref.getDigestFrequency()) {
                case IMMEDIATE -> digestFrequencyCombo.setSelectedIndex(0);
                case DAILY -> digestFrequencyCombo.setSelectedIndex(pref.getDigestHour() < 12 ? 2 : 3);
                case WEEKLY -> digestFrequencyCombo.setSelectedIndex(4);
                case NONE -> digestFrequencyCombo.setSelectedIndex(5);
            }
        } catch (Exception e) {
            // Log error or show subtle warning
        }
    }

    private String resolveUserId() {
        // Simple resolution for UI demo
        return currentUser.getUsername();
    }

    private JCheckBox emailEnabledCheckbox;
    private JCheckBox smsEnabledCheckbox;

    private JPanel createCategoryPreferencesCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Notification Categories");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel categoriesPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        categoriesPanel.setOpaque(false);

        String[] categories = {
                "Assignments & Tests",
                "Grades & Results",
                "Attendance Alerts",
                "Fee Reminders",
                "Announcements - Department",
                "Announcements - College",
                "Announcements - University",
                "Announcements - Societies",
                "Faculty Messages",
                "System Notifications"
        };

        for (String category : categories) {
            JCheckBox checkbox = new JCheckBox(category);
            checkbox.setFont(PastelTheme.BODY_FONT);
            checkbox.setOpaque(false);
            checkbox.setSelected(true); // Default: all enabled
            categoryCheckboxes.put(category, checkbox);
            categoriesPanel.add(checkbox);
        }

        card.add(categoriesPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDigestSettingsCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Digest Settings");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel settingsPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        settingsPanel.setOpaque(false);

        JLabel frequencyLabel = new JLabel("Email Digest Frequency:");
        frequencyLabel.setFont(PastelTheme.BODY_FONT);
        settingsPanel.add(frequencyLabel);

        digestFrequencyCombo = new JComboBox<>(new String[] {
                "Immediate (Real-time)",
                "Hourly Digest",
                "Daily Digest (Morning)",
                "Daily Digest (Evening)",
                "Weekly Digest",
                "Disabled"
        });
        digestFrequencyCombo.setFont(PastelTheme.BODY_FONT);
        digestFrequencyCombo.setSelectedIndex(2); // Default: Daily Morning
        settingsPanel.add(digestFrequencyCombo);

        emailEnabledCheckbox = new JCheckBox("Enable Email Notifications");
        emailEnabledCheckbox.setFont(PastelTheme.BODY_FONT);
        emailEnabledCheckbox.setOpaque(false);
        settingsPanel.add(emailEnabledCheckbox);

        smsEnabledCheckbox = new JCheckBox("Enable SMS Notifications");
        smsEnabledCheckbox.setFont(PastelTheme.BODY_FONT);
        smsEnabledCheckbox.setOpaque(false);
        settingsPanel.add(smsEnabledCheckbox);

        card.add(settingsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSaveButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);

        JButton saveBtn = new JButton("Save Preferences");
        PastelTheme.styleButtonPrimary(saveBtn);
        saveBtn.addActionListener(e -> savePreferences());
        panel.add(saveBtn);

        JButton resetBtn = new JButton("Reset to Defaults");
        PastelTheme.styleButtonSecondary(resetBtn);
        resetBtn.addActionListener(e -> resetToDefaults());
        panel.add(resetBtn);

        return panel;
    }

    private void savePreferences() {
        try {
            String userId = resolveUserId();
            NotificationPreference.DigestFrequency freq = NotificationPreference.DigestFrequency.IMMEDIATE;
            int hour = 8;

            int selected = digestFrequencyCombo.getSelectedIndex();
            switch (selected) {
                case 0 -> freq = NotificationPreference.DigestFrequency.IMMEDIATE;
                case 1 -> {
                    freq = NotificationPreference.DigestFrequency.DAILY;
                    hour = 12;
                } // Hourly approx
                case 2 -> {
                    freq = NotificationPreference.DigestFrequency.DAILY;
                    hour = 8;
                }
                case 3 -> {
                    freq = NotificationPreference.DigestFrequency.DAILY;
                    hour = 18;
                }
                case 4 -> freq = NotificationPreference.DigestFrequency.WEEKLY;
                case 5 -> freq = NotificationPreference.DigestFrequency.NONE;
            }

            NotificationPreference pref = new NotificationPreference(
                    userId,
                    freq,
                    hour,
                    emailEnabledCheckbox.isSelected(),
                    smsEnabledCheckbox.isSelected(),
                    LocalDateTime.now());

            DatabaseUtil.saveNotificationPreference(pref);

            JOptionPane.showMessageDialog(this,
                    "Notification preferences saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving preferences: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetToDefaults() {
        for (JCheckBox checkbox : categoryCheckboxes.values()) {
            checkbox.setSelected(true);
        }
        digestFrequencyCombo.setSelectedIndex(2);
        JOptionPane.showMessageDialog(this,
                "Preferences reset to defaults",
                "Reset",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

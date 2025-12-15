package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
    }

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

        JCheckBox quietHoursCheckbox = new JCheckBox("Enable Quiet Hours (10 PM - 8 AM)");
        quietHoursCheckbox.setFont(PastelTheme.BODY_FONT);
        quietHoursCheckbox.setOpaque(false);
        quietHoursCheckbox.setSelected(true);
        settingsPanel.add(quietHoursCheckbox);

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
        // TODO: Save to database
        JOptionPane.showMessageDialog(this,
                "Notification preferences saved successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
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

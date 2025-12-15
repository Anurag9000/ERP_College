package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Theme and accessibility customization panel
 */
public class ThemeCustomizationPanel extends JPanel {

    private final User currentUser;
    private JComboBox<String> themeCombo;
    private JSlider fontSizeSlider;
    private JCheckBox highContrastCheckbox;
    private JComboBox<String> colorBlindModeCombo;

    public ThemeCustomizationPanel(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Theme & Accessibility");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        mainPanel.add(createThemeCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createAccessibilityCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createSavePanel());

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
    }

    private JPanel createThemeCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Visual Theme");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel settingsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        settingsPanel.setOpaque(false);

        // Theme selection
        settingsPanel.add(new JLabel("Color Theme:"));
        themeCombo = new JComboBox<>(new String[] {
                "Pastel (Default)",
                "Dark Mode",
                "High Contrast Light",
                "High Contrast Dark",
                "Classic Blue",
                "Warm Tones"
        });
        themeCombo.setFont(PastelTheme.BODY_FONT);
        settingsPanel.add(themeCombo);

        // Font size
        settingsPanel.add(new JLabel("Font Size:"));
        fontSizeSlider = new JSlider(10, 20, 14);
        fontSizeSlider.setMajorTickSpacing(2);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);
        fontSizeSlider.setOpaque(false);
        settingsPanel.add(fontSizeSlider);

        // Preview
        settingsPanel.add(new JLabel("Preview:"));
        JLabel previewLabel = new JLabel("Sample Text (Aa Bb Cc 123)");
        previewLabel.setFont(new Font("Segoe UI", Font.PLAIN, fontSizeSlider.getValue()));
        fontSizeSlider.addChangeListener(e -> {
            previewLabel.setFont(new Font("Segoe UI", Font.PLAIN, fontSizeSlider.getValue()));
        });
        settingsPanel.add(previewLabel);

        card.add(settingsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAccessibilityCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Accessibility Options");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        optionsPanel.setOpaque(false);

        // High contrast
        highContrastCheckbox = new JCheckBox("Enable High Contrast Mode");
        highContrastCheckbox.setFont(PastelTheme.BODY_FONT);
        highContrastCheckbox.setOpaque(false);
        optionsPanel.add(highContrastCheckbox);

        // Color blind mode
        JPanel colorBlindPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorBlindPanel.setOpaque(false);
        colorBlindPanel.add(new JLabel("Color Blind Mode:"));
        colorBlindModeCombo = new JComboBox<>(new String[] {
                "None",
                "Protanopia (Red-Blind)",
                "Deuteranopia (Green-Blind)",
                "Tritanopia (Blue-Blind)",
                "Monochromacy (Grayscale)"
        });
        colorBlindModeCombo.setFont(PastelTheme.BODY_FONT);
        colorBlindPanel.add(colorBlindModeCombo);
        optionsPanel.add(colorBlindPanel);

        // Other options
        JCheckBox reducedMotionCheckbox = new JCheckBox("Reduce Motion/Animations");
        reducedMotionCheckbox.setFont(PastelTheme.BODY_FONT);
        reducedMotionCheckbox.setOpaque(false);
        optionsPanel.add(reducedMotionCheckbox);

        JCheckBox screenReaderCheckbox = new JCheckBox("Screen Reader Optimizations");
        screenReaderCheckbox.setFont(PastelTheme.BODY_FONT);
        screenReaderCheckbox.setOpaque(false);
        optionsPanel.add(screenReaderCheckbox);

        card.add(optionsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSavePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);

        JButton applyBtn = new JButton("Apply Changes");
        PastelTheme.styleButtonPrimary(applyBtn);
        applyBtn.addActionListener(e -> applyTheme());

        JButton resetBtn = new JButton("Reset to Default");
        PastelTheme.styleButtonSecondary(resetBtn);
        resetBtn.addActionListener(e -> resetTheme());

        panel.add(applyBtn);
        panel.add(resetBtn);

        return panel;
    }

    private void applyTheme() {
        String selectedTheme = (String) themeCombo.getSelectedItem();
        int fontSize = fontSizeSlider.getValue();

        JOptionPane.showMessageDialog(this,
                "Theme applied: " + selectedTheme + "\nFont size: " + fontSize + "pt\n\n" +
                        "Note: Some changes require application restart.",
                "Theme Applied",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetTheme() {
        themeCombo.setSelectedIndex(0);
        fontSizeSlider.setValue(14);
        highContrastCheckbox.setSelected(false);
        colorBlindModeCombo.setSelectedIndex(0);

        JOptionPane.showMessageDialog(this,
                "Theme reset to default settings",
                "Reset Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

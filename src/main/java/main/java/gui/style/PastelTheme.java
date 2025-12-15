package main.java.gui.style;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Defines the Pastel UI Design System.
 */
public class PastelTheme {
    // Soft off-white background
    public static final Color PASTEL_BG = new Color(250, 250, 252);
    public static final Color CARD_BG = Color.WHITE;

    // Pastel Accents
    public static final Color PASTEL_BLUE = new Color(219, 234, 254);
    public static final Color PASTEL_BLUE_DARK = new Color(59, 130, 246);

    public static final Color PASTEL_RED = new Color(254, 226, 226);
    public static final Color PASTEL_RED_DARK = new Color(220, 38, 38);

    public static final Color PASTEL_GREEN = new Color(220, 252, 231);
    public static final Color PASTEL_GREEN_DARK = new Color(22, 163, 74);

    public static final Color PASTEL_YELLOW = new Color(254, 249, 195);
    public static final Color PASTEL_YELLOW_DARK = new Color(202, 138, 4);

    public static final Color PASTEL_PURPLE = new Color(243, 232, 255);
    public static final Color PASTEL_PURPLE_DARK = new Color(147, 51, 234);

    // Text Colors
    public static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);

    // Fonts
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font SUBHEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font CARD_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);

    /**
     * Styles a panel as a "Card" with shadow-like border.
     */
    public static void styleCard(JPanel panel) {
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    }

    /**
     * Styles a primary action button.
     */
    public static void styleButtonPrimary(JButton button) {
        button.setBackground(PASTEL_BLUE_DARK);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Styles a secondary action button.
     */
    public static void styleButtonSecondary(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Returns a colored dot icon.
     */
    public static Icon getDotIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, getIconWidth(), getIconHeight());
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }
        };
    }
}

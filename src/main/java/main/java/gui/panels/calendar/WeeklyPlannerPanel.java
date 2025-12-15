package main.java.gui.panels.calendar;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Weekly planner view for calendar
 */
public class WeeklyPlannerPanel extends JPanel {

    private final User currentUser;
    private LocalDate currentWeekStart;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");

    public WeeklyPlannerPanel(User currentUser) {
        this.currentUser = currentUser;
        this.currentWeekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);

        setLayout(new BorderLayout(10, 10));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header with navigation
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Week grid
        add(createWeekGrid(), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel weekLabel = new JLabel("Week of " + currentWeekStart.format(DATE_FORMAT));
        weekLabel.setFont(PastelTheme.CARD_TITLE_FONT);
        panel.add(weekLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        navPanel.setOpaque(false);

        JButton prevBtn = new JButton("← Previous");
        JButton todayBtn = new JButton("Today");
        JButton nextBtn = new JButton("Next →");

        PastelTheme.styleButtonSecondary(prevBtn);
        PastelTheme.styleButtonSecondary(todayBtn);
        PastelTheme.styleButtonSecondary(nextBtn);

        prevBtn.addActionListener(e -> navigateWeek(-1));
        todayBtn.addActionListener(e -> navigateToToday());
        nextBtn.addActionListener(e -> navigateWeek(1));

        navPanel.add(prevBtn);
        navPanel.add(todayBtn);
        navPanel.add(nextBtn);

        panel.add(navPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createWeekGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 7, 10, 0));
        grid.setOpaque(false);

        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            grid.add(createDayColumn(days[i], date));
        }

        return grid;
    }

    private JPanel createDayColumn(String dayName, LocalDate date) {
        JCard card = new JCard(new BorderLayout());

        // Day header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel dayLabel = new JLabel(dayName);
        dayLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerPanel.add(dayLabel, BorderLayout.NORTH);

        JLabel dateLabel = new JLabel(date.format(DATE_FORMAT));
        dateLabel.setFont(PastelTheme.BODY_FONT);
        dateLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        headerPanel.add(dateLabel, BorderLayout.SOUTH);

        card.add(headerPanel, BorderLayout.NORTH);

        // Events list
        JPanel eventsPanel = new JPanel();
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
        eventsPanel.setOpaque(false);

        // Mock events
        if (date.getDayOfWeek().getValue() < 6) { // Weekdays
            eventsPanel.add(createEventBlock("CS101 Lecture", "9:00 AM", PastelTheme.PASTEL_BLUE_DARK));
            eventsPanel.add(Box.createVerticalStrut(5));
            eventsPanel.add(createEventBlock("Lab Session", "2:00 PM", PastelTheme.PASTEL_GREEN_DARK));
        }

        card.add(new JScrollPane(eventsPanel), BorderLayout.CENTER);
        return card;
    }

    private JPanel createEventBlock(String title, String time, Color color) {
        JPanel block = new JPanel(new BorderLayout());
        block.setBackground(color);
        block.setBorder(new EmptyBorder(5, 8, 5, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(Color.WHITE);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(Color.WHITE);

        block.add(titleLabel, BorderLayout.NORTH);
        block.add(timeLabel, BorderLayout.SOUTH);

        return block;
    }

    private void navigateWeek(int weeks) {
        currentWeekStart = currentWeekStart.plusWeeks(weeks);
        refreshView();
    }

    private void navigateToToday() {
        currentWeekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        refreshView();
    }

    private void refreshView() {
        removeAll();
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createWeekGrid(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}

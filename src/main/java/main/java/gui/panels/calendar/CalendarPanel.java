package main.java.gui.panels.calendar;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Section;
import main.java.models.User;
import main.java.service.StudentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A Pastel UI Smart Calendar.
 */
public class CalendarPanel extends JPanel {

    private final User user;
    private YearMonth currentMonth;
    private JPanel gridPanel;
    private JLabel monthLabel;

    // In-memory cache for personal events (MVP)
    private final List<String> personalEvents = new ArrayList<>();

    public CalendarPanel(User user) {
        this.user = user;
        this.currentMonth = YearMonth.now();

        setLayout(new BorderLayout(20, 20)); // Generous gap
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        monthLabel = new JLabel();
        monthLabel.setFont(PastelTheme.HEADER_FONT);
        monthLabel.setForeground(PastelTheme.TEXT_PRIMARY);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.setOpaque(false);

        JButton prevBtn = new JButton("<");
        PastelTheme.styleButtonSecondary(prevBtn);
        prevBtn.addActionListener(e -> changeMonth(-1));

        JButton nextBtn = new JButton(">");
        PastelTheme.styleButtonSecondary(nextBtn);
        nextBtn.addActionListener(e -> changeMonth(1));

        JButton todayBtn = new JButton("Today");
        PastelTheme.styleButtonSecondary(todayBtn);
        todayBtn.addActionListener(e -> {
            currentMonth = YearMonth.now();
            updateStats();
        });

        controls.add(prevBtn);
        controls.add(todayBtn);
        controls.add(nextBtn);

        header.add(monthLabel, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private void changeMonth(int months) {
        currentMonth = currentMonth.plusMonths(months);
        updateStats();
    }

    private JPanel createMainContent() {
        // Grid Layout: 7 days columns
        gridPanel = new JPanel(new GridLayout(0, 7, 10, 10));
        gridPanel.setOpaque(false);
        updateStats(); // Initial population
        return gridPanel;
    }

    private void updateStats() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        gridPanel.removeAll();

        // Add Weekday Headers
        for (DayOfWeek day : DayOfWeek.values()) {
            JLabel header = new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());
            header.setHorizontalAlignment(SwingConstants.CENTER);
            header.setFont(PastelTheme.CARD_TITLE_FONT);
            header.setForeground(PastelTheme.TEXT_SECONDARY);
            gridPanel.add(header);
        }

        // Calculate days
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeekOffset = firstOfMonth.getDayOfWeek().getValue() - 1; // Mon=0, Sun=6
        int daysInMonth = currentMonth.lengthOfMonth();

        // Fetched schedule
        List<Section> sections = new ArrayList<>();
        try {
            if (user != null && "Student".equalsIgnoreCase(user.getRole()))
                sections = StudentService.getSchedule(user);
        } catch (Exception ignored) {
        }

        // Padding for previous month
        for (int i = 0; i < dayOfWeekOffset; i++) {
            gridPanel.add(new JLabel("")); // Empty placeholder
        }

        // Actual Days
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            DayCell cell = new DayCell(date);

            // Auto-fill recurring classes
            DayOfWeek dow = date.getDayOfWeek();
            List<Section> daysClasses = sections.stream()
                    .filter(s -> s.getDayOfWeek() == dow)
                    .collect(Collectors.toList());

            for (Section s : daysClasses) {
                cell.addEvent(s.getCourseId() + " (" + s.getStartTime() + ")", PastelTheme.PASTEL_BLUE);
            }

            // Add a simple logic for today highlight
            if (date.equals(LocalDate.now())) {
                cell.markToday();
            }

            gridPanel.add(cell);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    /**
     * Inner class representing a single day cell in the calendar.
     */
    private class DayCell extends JCard {
        private final LocalDate date;
        private final JPanel eventContainer;

        public DayCell(LocalDate date) {
            super(new BorderLayout());
            this.date = date;

            JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
            dayLabel.setFont(PastelTheme.BODY_FONT);
            dayLabel.setForeground(PastelTheme.TEXT_SECONDARY);
            dayLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            add(dayLabel, BorderLayout.NORTH);

            eventContainer = new JPanel();
            eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
            eventContainer.setOpaque(false);
            add(eventContainer, BorderLayout.CENTER);

            // Make it clickable to add event
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        String note = JOptionPane.showInputDialog(DayCell.this, "Add Note for " + date);
                        if (note != null && !note.trim().isEmpty()) {
                            addEvent(note, PastelTheme.PASTEL_YELLOW);
                        }
                    }
                }
            });
        }

        public void markToday() {
            setBorder(BorderFactory.createLineBorder(PastelTheme.PASTEL_BLUE_DARK, 2));
            setBackground(PastelTheme.PASTEL_BLUE);
        }

        public void addEvent(String text, Color color) {
            JLabel eventLabel = new JLabel("• " + text);
            eventLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            eventLabel.setOpaque(true);
            eventLabel.setBackground(color);
            eventLabel.setBorder(new EmptyBorder(2, 4, 2, 4));

            // Round corners for event pill? Simple opaque for now.
            eventContainer.add(eventLabel);
            eventContainer.add(Box.createVerticalStrut(2));
        }
    }
}

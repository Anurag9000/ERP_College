package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Faculty;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Meeting slots management for students and faculty
 */
public class MeetingSlotsPanel extends JPanel {

    private final User currentUser;
    private DefaultTableModel slotsModel;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public MeetingSlotsPanel(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Meeting Slots");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(PastelTheme.BODY_FONT);

        tabs.addTab("Request Meeting", createRequestPanel());
        tabs.addTab("My Meetings", createMyMeetingsPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PastelTheme.PASTEL_BG);

        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Request Faculty Meeting");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setOpaque(false);

        // Faculty selection
        formPanel.add(new JLabel("Select Faculty:"));
        JComboBox<String> facultyCombo = new JComboBox<>();
        List<Faculty> allFaculty = DatabaseUtil.getAllFaculty();
        for (Faculty f : allFaculty) {
            facultyCombo.addItem(f.getFirstName() + " " + f.getLastName());
        }
        facultyCombo.setFont(PastelTheme.BODY_FONT);
        formPanel.add(facultyCombo);

        // Date selection
        formPanel.add(new JLabel("Preferred Date:"));
        JComboBox<String> dateCombo = new JComboBox<>();
        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            dateCombo.addItem(date.toString());
        }
        dateCombo.setFont(PastelTheme.BODY_FONT);
        formPanel.add(dateCombo);

        // Time selection
        formPanel.add(new JLabel("Preferred Time:"));
        JComboBox<String> timeCombo = new JComboBox<>(new String[] {
                "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
                "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        });
        timeCombo.setFont(PastelTheme.BODY_FONT);
        formPanel.add(timeCombo);

        // Duration
        formPanel.add(new JLabel("Duration:"));
        JComboBox<String> durationCombo = new JComboBox<>(new String[] {
                "15 minutes", "30 minutes", "45 minutes", "1 hour"
        });
        durationCombo.setFont(PastelTheme.BODY_FONT);
        formPanel.add(durationCombo);

        // Purpose
        formPanel.add(new JLabel("Purpose:"));
        JTextField purposeField = new JTextField();
        purposeField.setFont(PastelTheme.BODY_FONT);
        formPanel.add(purposeField);

        card.add(formPanel, BorderLayout.CENTER);

        JButton requestBtn = new JButton("Send Meeting Request");
        PastelTheme.styleButtonPrimary(requestBtn);
        requestBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Meeting request sent to " + facultyCombo.getSelectedItem() + "\n" +
                            "You will be notified once the faculty approves.",
                    "Request Sent",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(requestBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMyMeetingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PastelTheme.PASTEL_BG);

        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Faculty", "Date", "Time", "Duration", "Status", "Action" };
        slotsModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(slotsModel);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        // Mock data
        slotsModel.addRow(new Object[] {
                "Dr. John Smith", "2025-12-16", "10:00 AM", "30 min", "Pending", "Cancel"
        });
        slotsModel.addRow(new Object[] {
                "Dr. Jane Doe", "2025-12-18", "02:00 PM", "45 min", "Approved", "Join"
        });

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }
}

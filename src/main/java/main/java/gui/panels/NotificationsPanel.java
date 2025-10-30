package main.java.gui.panels;

import main.java.models.NotificationMessage;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel to review and broadcast system notifications.
 */
public class NotificationsPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JComboBox<String> audienceFilter;
    private JTable notificationTable;
    private DefaultTableModel tableModel;
    private JButton broadcastButton;

    public NotificationsPanel() {
        initializeComponents();
        setupLayout();
        setupHandlers();
        loadNotifications();
    }

    private void initializeComponents() {
        audienceFilter = new JComboBox<>(new String[]{"All", "Students", "Instructors", "Admins"});
        broadcastButton = createButton("Broadcast Message", new Color(37, 99, 235));

        tableModel = new DefaultTableModel(new Object[]{"Time", "Audience", "Category", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        notificationTable = new JTable(tableModel);
        notificationTable.setRowHeight(24);
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Notification Center");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(new JLabel("Audience:"));
        controls.add(audienceFilter);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(broadcastButton);

        header.add(title, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        JScrollPane tableScroll = new JScrollPane(notificationTable);

        add(header, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private void setupHandlers() {
        audienceFilter.addActionListener(e -> loadNotifications());
        broadcastButton.addActionListener(e -> broadcastMessage());
    }

    private void loadNotifications() {
        tableModel.setRowCount(0);

        NotificationMessage.Audience audience = NotificationMessage.Audience.ALL;
        switch ((String) audienceFilter.getSelectedItem()) {
            case "Students":
                audience = NotificationMessage.Audience.STUDENT;
                break;
            case "Instructors":
                audience = NotificationMessage.Audience.INSTRUCTOR;
                break;
            case "Admins":
                audience = NotificationMessage.Audience.ADMIN;
                break;
            default:
                break;
        }

        List<NotificationMessage> notifications = DatabaseUtil.getNotifications(audience, null);
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        for (NotificationMessage message : notifications) {
            tableModel.addRow(new Object[]{
                    message.getCreatedAt().format(FORMATTER),
                    message.getAudience().name(),
                    message.getCategory(),
                    message.getMessage()
            });
        }
    }

    private void broadcastMessage() {
        JTextField categoryField = new JTextField("General");
        JTextArea messageArea = new JTextArea(4, 25);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new GridLayout(2, 1, 6, 6));
        top.add(new JLabel("Category:"));
        top.add(categoryField);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Broadcast Notification",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String messageText = messageArea.getText().trim();
        if (messageText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Message cannot be empty.");
            return;
        }

        String category = categoryField.getText().trim().isEmpty() ? "General" : categoryField.getText().trim();
        NotificationMessage notification = new NotificationMessage(
                NotificationMessage.Audience.ALL,
                null,
                messageText,
                category
        );
        DatabaseUtil.addNotification(notification);
        JOptionPane.showMessageDialog(this, "Notification broadcasted.");
        loadNotifications();
    }
}

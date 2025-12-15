package main.java.gui.panels;

import main.java.data.dao.AnnouncementDao;
import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Announcement;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Announcements hub with category filtering
 */
public class AnnouncementsHubPanel extends JPanel {

    private final User currentUser;
    private final AnnouncementDao announcementDao;
    private JPanel announcementsContainer;
    private Announcement.Category selectedCategory = null;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public AnnouncementsHubPanel(User currentUser) {
        this.currentUser = currentUser;
        this.announcementDao = new AnnouncementDao();

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header with filters
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Announcements list
        announcementsContainer = new JPanel();
        announcementsContainer.setLayout(new BoxLayout(announcementsContainer, BoxLayout.Y_AXIS));
        announcementsContainer.setBackground(PastelTheme.PASTEL_BG);

        JScrollPane scrollPane = new JScrollPane(announcementsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        loadAnnouncements();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        JLabel header = new JLabel("Announcements");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        panel.add(header, BorderLayout.WEST);

        // Category filters
        JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filtersPanel.setOpaque(false);

        JButton allBtn = new JButton("All");
        PastelTheme.styleButtonSecondary(allBtn);
        allBtn.addActionListener(e -> {
            selectedCategory = null;
            loadAnnouncements();
        });
        filtersPanel.add(allBtn);

        for (Announcement.Category cat : Announcement.Category.values()) {
            JButton btn = new JButton(cat.name());
            PastelTheme.styleButtonSecondary(btn);
            btn.addActionListener(e -> {
                selectedCategory = cat;
                loadAnnouncements();
            });
            filtersPanel.add(btn);
        }

        panel.add(filtersPanel, BorderLayout.EAST);
        return panel;
    }

    private void loadAnnouncements() {
        announcementsContainer.removeAll();

        List<Announcement> announcements;
        if (selectedCategory == null) {
            announcements = announcementDao.getAllAnnouncements();
        } else {
            announcements = announcementDao.getAnnouncementsByCategory(selectedCategory);
        }

        if (announcements.isEmpty()) {
            JLabel emptyLabel = new JLabel("No announcements available");
            emptyLabel.setFont(PastelTheme.BODY_FONT);
            emptyLabel.setForeground(PastelTheme.TEXT_SECONDARY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            announcementsContainer.add(Box.createVerticalStrut(50));
            announcementsContainer.add(emptyLabel);
        } else {
            for (Announcement announcement : announcements) {
                announcementsContainer.add(createAnnouncementCard(announcement));
                announcementsContainer.add(Box.createVerticalStrut(15));
            }
        }

        announcementsContainer.revalidate();
        announcementsContainer.repaint();
    }

    private JPanel createAnnouncementCard(Announcement announcement) {
        JCard card = new JCard(new BorderLayout(10, 10));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Header with category badge
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(announcement.getTitle());
        titleLabel.setFont(PastelTheme.CARD_TITLE_FONT);
        titleLabel.setForeground(PastelTheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel categoryBadge = new JLabel(announcement.getCategory().name());
        categoryBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        categoryBadge.setForeground(Color.WHITE);
        categoryBadge.setOpaque(true);
        categoryBadge.setBackground(getCategoryColor(announcement.getCategory()));
        categoryBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
        headerPanel.add(categoryBadge, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);

        // Content
        JTextArea contentArea = new JTextArea(announcement.getContent());
        contentArea.setFont(PastelTheme.BODY_FONT);
        contentArea.setForeground(PastelTheme.TEXT_SECONDARY);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);
        contentArea.setRows(3);
        card.add(contentArea, BorderLayout.CENTER);

        // Footer with metadata
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        JLabel metaLabel = new JLabel(String.format("Posted by %s on %s",
                announcement.getPostedBy(),
                announcement.getPostedAt().format(DATE_FORMAT)));
        metaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        metaLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        footerPanel.add(metaLabel, BorderLayout.WEST);

        if (announcement.getPriority() == Announcement.Priority.HIGH) {
            JLabel priorityLabel = new JLabel("⚠ HIGH PRIORITY");
            priorityLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            priorityLabel.setForeground(PastelTheme.PASTEL_RED_DARK);
            footerPanel.add(priorityLabel, BorderLayout.EAST);
        }

        card.add(footerPanel, BorderLayout.SOUTH);

        return card;
    }

    private Color getCategoryColor(Announcement.Category category) {
        switch (category) {
            case DEPARTMENT:
                return PastelTheme.PASTEL_BLUE_DARK;
            case UNION:
                return PastelTheme.PASTEL_PURPLE_DARK;
            case COLLEGE:
                return PastelTheme.PASTEL_GREEN_DARK;
            case UNIVERSITY:
                return PastelTheme.PASTEL_YELLOW_DARK;
            case SOCIETY:
                return PastelTheme.PASTEL_RED_DARK;
            default:
                return Color.GRAY;
        }
    }
}

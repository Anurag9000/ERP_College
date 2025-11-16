package main.java.gui.panels;

import main.java.models.EnrollmentRecord;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.InstructorService;
import main.java.utils.DatabaseUtil;
import main.java.data.dao.InstructorMessageDao;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Messaging workspace for instructors to contact enrolled students.
 */
public class InstructorMessagingPanel extends JPanel {
    private final User instructor;
    private final JComboBox<SectionOption> sectionCombo;
    private final DefaultTableModel rosterModel;
    private final JTable rosterTable;
    private final JTextField subjectField;
    private final JTextArea bodyArea;
    private final JLabel recipientCountLabel;
    private final JButton sendButton;
    private final DefaultTableModel historyModel;
    private final JTable historyTable;

    public InstructorMessagingPanel(User instructor) {
        this.instructor = instructor;
        this.sectionCombo = new JComboBox<>();
        this.rosterModel = new DefaultTableModel(new Object[]{"Send", "Student ID", "Name"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        this.rosterTable = new JTable(rosterModel);
        rosterTable.setRowHeight(22);
        rosterModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
                updateRecipientCount();
            }
        });
        this.subjectField = new JTextField();
        this.bodyArea = new JTextArea(5, 40);
        this.bodyArea.setLineWrap(true);
        this.bodyArea.setWrapStyleWord(true);
        this.recipientCountLabel = new JLabel("Recipients: 0");
        this.sendButton = new JButton("Send Message");
        this.historyModel = new DefaultTableModel(new Object[]{"Sent", "Section", "Recipients", "Subject", "Preview"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.historyTable = new JTable(historyModel);
        historyTable.setRowHeight(22);

        buildLayout();
        hookListeners();
        refreshSections();
        refreshHistory();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        header.add(new JLabel("Section:"));
        header.add(sectionCombo);
        JButton refreshButton = new JButton("Refresh Roster");
        refreshButton.addActionListener(e -> loadRoster());
        header.add(refreshButton);
        header.add(recipientCountLabel);

        JScrollPane rosterScroll = new JScrollPane(rosterTable);
        rosterScroll.setBorder(BorderFactory.createTitledBorder("Enrolled Students"));

        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton selectAllButton = new JButton("Select All");
        JButton clearButton = new JButton("Clear");
        selectAllButton.addActionListener(e -> setAllRecipients(true));
        clearButton.addActionListener(e -> setAllRecipients(false));
        selectionButtons.add(selectAllButton);
        selectionButtons.add(clearButton);

        JPanel composePanel = new JPanel(new BorderLayout(6, 6));
        composePanel.setBorder(BorderFactory.createTitledBorder("Compose Message"));
        JPanel subjectRow = new JPanel(new BorderLayout(6, 6));
        subjectRow.add(new JLabel("Subject:"), BorderLayout.WEST);
        subjectRow.add(subjectField, BorderLayout.CENTER);
        composePanel.add(subjectRow, BorderLayout.NORTH);
        composePanel.add(new JScrollPane(bodyArea), BorderLayout.CENTER);
        JPanel composeActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        sendButton.setBackground(new Color(34, 197, 94));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        composeActions.add(sendButton);
        composePanel.add(composeActions, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(header, BorderLayout.NORTH);
        leftPanel.add(rosterScroll, BorderLayout.CENTER);
        leftPanel.add(selectionButtons, BorderLayout.SOUTH);

        JPanel leftContainer = new JPanel(new BorderLayout(10, 10));
        leftContainer.add(leftPanel, BorderLayout.CENTER);
        leftContainer.add(composePanel, BorderLayout.SOUTH);

        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Recent Messages"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, historyScroll);
        splitPane.setResizeWeight(0.6);

        add(splitPane, BorderLayout.CENTER);
    }

    private void hookListeners() {
        sectionCombo.addActionListener(e -> loadRoster());
        sendButton.addActionListener(e -> sendMessage());
    }

    private void refreshSections() {
        sectionCombo.removeAllItems();
        List<Section> sections = InstructorService.getAssignedSections(instructor);
        for (Section section : sections) {
            sectionCombo.addItem(new SectionOption(section));
        }
        if (sectionCombo.getItemCount() > 0) {
            sectionCombo.setSelectedIndex(0);
        }
        loadRoster();
    }

    private void loadRoster() {
        rosterModel.setRowCount(0);
        Section section = getSelectedSection();
        if (section == null) {
            updateRecipientCount();
            return;
        }
        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentsForSection(section.getSectionId());
        enrollments.stream()
                .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                .forEach(rec -> {
                    Student student = DatabaseUtil.getStudent(rec.getStudentId());
                    String name = student != null ? student.getFullName() : rec.getStudentId();
                    rosterModel.addRow(new Object[]{Boolean.TRUE, rec.getStudentId(), name});
                });
        updateRecipientCount();
    }

    private Section getSelectedSection() {
        SectionOption option = (SectionOption) sectionCombo.getSelectedItem();
        return option != null ? option.section() : null;
    }

    private void setAllRecipients(boolean value) {
        for (int i = 0; i < rosterModel.getRowCount(); i++) {
            rosterModel.setValueAt(value, i, 0);
        }
        updateRecipientCount();
    }

    private void updateRecipientCount() {
        int count = 0;
        for (int i = 0; i < rosterModel.getRowCount(); i++) {
            Boolean selected = (Boolean) rosterModel.getValueAt(i, 0);
            if (selected != null && selected) {
                count++;
            }
        }
        recipientCountLabel.setText("Recipients: " + count);
    }

    private List<String> collectSelectedRecipients() {
        List<String> recipients = new ArrayList<>();
        for (int row = 0; row < rosterModel.getRowCount(); row++) {
            Boolean selected = (Boolean) rosterModel.getValueAt(row, 0);
            if (selected != null && selected) {
                recipients.add((String) rosterModel.getValueAt(row, 1));
            }
        }
        return recipients;
    }

    private void sendMessage() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        if (DatabaseUtil.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this, "Messaging is disabled during maintenance mode.");
            return;
        }
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();
        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject is required.");
            return;
        }
        if (body.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Message body is required.");
            return;
        }
        List<String> recipients = collectSelectedRecipients();
        try {
            DatabaseUtil.sendInstructorMessage(instructor, section.getSectionId(), recipients, subject, body);
            JOptionPane.showMessageDialog(this, "Message sent to " + recipients.size() + " recipient(s).");
            refreshHistory();
            subjectField.setText("");
            bodyArea.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to send message", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        List<InstructorMessageDao.MessageLog> history =
                DatabaseUtil.getInstructorMessageLog(instructor.getUsername());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm");
        for (InstructorMessageDao.MessageLog log : history) {
            List<String> recipients = parseRecipients(log.recipientIds());
            historyModel.addRow(new Object[]{
                    log.createdAt().format(formatter),
                    log.sectionId(),
                    recipients.size() + " students",
                    log.subject(),
                    preview(log.body())
            });
        }
    }

    private List<String> parseRecipients(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.replace("\n", " ").trim();
        if (trimmed.length() <= 60) {
            return trimmed;
        }
        return trimmed.substring(0, 57) + "...";
    }

    private static final class SectionOption {
        private final Section section;

        SectionOption(Section section) {
            this.section = section;
        }

        Section section() {
            return section;
        }

        @Override
        public String toString() {
            return section.getSectionId() + " - " + section.getTitle();
        }
    }
}

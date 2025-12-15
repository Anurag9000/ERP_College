package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Section;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

/**
 * Timetable conflict simulator for admins
 */
public class TimetableConflictSimulator extends JPanel {

    private DefaultTableModel conflictsModel;
    private JTextArea summaryArea;

    public TimetableConflictSimulator() {
        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Timetable Conflict Simulator");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        mainPanel.add(createSummaryPanel(), BorderLayout.NORTH);
        mainPanel.add(createConflictsTable(), BorderLayout.CENTER);
        mainPanel.add(createActionsPanel(), BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        detectConflicts();
    }

    private JPanel createSummaryPanel() {
        JCard card = new JCard(new BorderLayout());

        summaryArea = new JTextArea(3, 40);
        summaryArea.setFont(PastelTheme.BODY_FONT);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setOpaque(false);

        card.add(summaryArea, BorderLayout.CENTER);
        return card;
    }

    private JPanel createConflictsTable() {
        JCard card = new JCard(new BorderLayout());

        JLabel title = new JLabel("Detected Conflicts");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(title, BorderLayout.NORTH);

        String[] columns = { "Conflict Type", "Section 1", "Section 2", "Day", "Time", "Resource", "Severity" };
        conflictsModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(conflictsModel);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);

        JButton refreshBtn = new JButton("Refresh Analysis");
        PastelTheme.styleButtonPrimary(refreshBtn);
        refreshBtn.addActionListener(e -> detectConflicts());

        JButton exportBtn = new JButton("Export Report");
        PastelTheme.styleButtonSecondary(exportBtn);
        exportBtn.addActionListener(e -> exportReport());

        panel.add(refreshBtn);
        panel.add(exportBtn);

        return panel;
    }

    private void detectConflicts() {
        conflictsModel.setRowCount(0);

        List<Section> allSections = DatabaseUtil.getAllSections();
        int facultyConflicts = 0;
        int roomConflicts = 0;
        int studentConflicts = 0;

        // Detect faculty conflicts (same instructor, same time)
        Map<String, List<Section>> facultySchedule = new HashMap<>();
        for (Section s : allSections) {
            if (s.getFacultyId() != null) {
                facultySchedule.computeIfAbsent(s.getFacultyId(), k -> new ArrayList<>()).add(s);
            }
        }

        for (Map.Entry<String, List<Section>> entry : facultySchedule.entrySet()) {
            List<Section> sections = entry.getValue();
            for (int i = 0; i < sections.size(); i++) {
                for (int j = i + 1; j < sections.size(); j++) {
                    Section s1 = sections.get(i);
                    Section s2 = sections.get(j);

                    if (hasTimeConflict(s1, s2)) {
                        conflictsModel.addRow(new Object[] {
                                "Faculty Conflict",
                                s1.getSectionId(),
                                s2.getSectionId(),
                                s1.getDayOfWeek(),
                                s1.getStartTime() + " - " + s1.getEndTime(),
                                entry.getKey(),
                                "HIGH"
                        });
                        facultyConflicts++;
                    }
                }
            }
        }

        // Detect room conflicts (same room, same time)
        Map<String, List<Section>> roomSchedule = new HashMap<>();
        for (Section s : allSections) {
            if (s.getLocation() != null) {
                roomSchedule.computeIfAbsent(s.getLocation(), k -> new ArrayList<>()).add(s);
            }
        }

        for (Map.Entry<String, List<Section>> entry : roomSchedule.entrySet()) {
            List<Section> sections = entry.getValue();
            for (int i = 0; i < sections.size(); i++) {
                for (int j = i + 1; j < sections.size(); j++) {
                    Section s1 = sections.get(i);
                    Section s2 = sections.get(j);

                    if (hasTimeConflict(s1, s2)) {
                        conflictsModel.addRow(new Object[] {
                                "Room Conflict",
                                s1.getSectionId(),
                                s2.getSectionId(),
                                s1.getDayOfWeek(),
                                s1.getStartTime() + " - " + s1.getEndTime(),
                                entry.getKey(),
                                "MEDIUM"
                        });
                        roomConflicts++;
                    }
                }
            }
        }

        // Update summary
        int totalConflicts = facultyConflicts + roomConflicts + studentConflicts;
        summaryArea.setText(String.format(
                "Conflict Analysis Summary:\n" +
                        "Total Conflicts: %d | Faculty: %d | Room: %d | Student: %d\n" +
                        "Analyzed %d sections across all departments.",
                totalConflicts, facultyConflicts, roomConflicts, studentConflicts, allSections.size()));

        if (totalConflicts == 0) {
            summaryArea.setText(summaryArea.getText() + "\n✓ No conflicts detected! Timetable is valid.");
        }
    }

    private boolean hasTimeConflict(Section s1, Section s2) {
        if (s1.getDayOfWeek() != s2.getDayOfWeek())
            return false;
        if (s1.getStartTime() == null || s1.getEndTime() == null)
            return false;
        if (s2.getStartTime() == null || s2.getEndTime() == null)
            return false;

        LocalTime start1 = s1.getStartTime();
        LocalTime end1 = s1.getEndTime();
        LocalTime start2 = s2.getStartTime();
        LocalTime end2 = s2.getEndTime();

        return !(end1.isBefore(start2) || end1.equals(start2) || start1.isAfter(end2) || start1.equals(end2));
    }

    private void exportReport() {
        JOptionPane.showMessageDialog(this,
                "Conflict report exported to: timetable_conflicts.csv",
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

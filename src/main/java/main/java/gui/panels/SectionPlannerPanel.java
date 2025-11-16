package main.java.gui.panels;

import main.java.models.Section;
import main.java.models.EnrollmentRecord;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Visual planner highlighting section clashes and capacity warnings.
 */
public class SectionPlannerPanel extends JPanel {
    private final DefaultTableModel sectionModel;
    private final DefaultTableModel conflictModel;
    private final DefaultTableModel warningModel;
    private final JTable sectionTable;
    private final JTable conflictTable;
    private final JTable warningTable;
    private final JComboBox<String> dayFilter;
    private final JTextField locationFilter;
    private Collection<Section> cachedSections = List.of();

    public SectionPlannerPanel() {
        this.sectionModel = new DefaultTableModel(new Object[]{
                "Section", "Course", "Day", "Time", "Room", "Capacity", "Enrolled"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.conflictModel = new DefaultTableModel(new Object[]{
                "Type", "Section A", "Section B", "Detail"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.warningModel = new DefaultTableModel(new Object[]{
                "Section", "Capacity", "Enrolled", "Over by"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.sectionTable = new JTable(sectionModel);
        sectionTable.setRowHeight(22);
        this.conflictTable = new JTable(conflictModel);
        conflictTable.setRowHeight(22);
        this.warningTable = new JTable(warningModel);
        warningTable.setRowHeight(22);
        this.dayFilter = new JComboBox<>(buildDayFilter());
        this.locationFilter = new JTextField();

        buildLayout();
        hookListeners();
        refreshData();
    }

    private void buildLayout() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filters.add(new JLabel("Day:"));
        filters.add(dayFilter);
        filters.add(new JLabel("Room contains:"));
        locationFilter.setPreferredSize(new Dimension(150, 24));
        filters.add(locationFilter);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        filters.add(refreshButton);

        JScrollPane sectionScroll = new JScrollPane(sectionTable);
        sectionScroll.setBorder(BorderFactory.createTitledBorder("Sections"));

        JScrollPane conflictScroll = new JScrollPane(conflictTable);
        conflictScroll.setBorder(BorderFactory.createTitledBorder("Detected Clashes"));

        JScrollPane warningScroll = new JScrollPane(warningTable);
        warningScroll.setBorder(BorderFactory.createTitledBorder("Capacity Warnings"));

        JSplitPane warningsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, conflictScroll, warningScroll);
        warningsPane.setResizeWeight(0.5);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sectionScroll, warningsPane);
        splitPane.setResizeWeight(0.55);

        add(filters, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void hookListeners() {
        dayFilter.addActionListener(e -> applyFilters());
        locationFilter.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilters));
    }

    private void refreshData() {
        this.cachedSections = DatabaseUtil.getAllSections();
        populateConflicts();
        populateWarnings();
        applyFilters();
    }

    private void populateConflicts() {
        conflictModel.setRowCount(0);
        List<DatabaseUtil.SectionConflict> conflicts = DatabaseUtil.findSectionConflicts();
        for (DatabaseUtil.SectionConflict conflict : conflicts) {
            conflictModel.addRow(new Object[]{
                    conflict.type().name(),
                    conflict.sectionA(),
                    conflict.sectionB(),
                    conflict.detail()
            });
        }
    }

    private void populateWarnings() {
        warningModel.setRowCount(0);
        List<DatabaseUtil.CapacityWarning> warnings = DatabaseUtil.findCapacityWarnings();
        for (DatabaseUtil.CapacityWarning warning : warnings) {
            warningModel.addRow(new Object[]{
                    warning.sectionId(),
                    warning.capacity(),
                    warning.enrolled(),
                    warning.overBy()
            });
        }
    }

    private void applyFilters() {
        sectionModel.setRowCount(0);
        String daySelection = (String) dayFilter.getSelectedItem();
        DayOfWeek targetDay = resolveDay(daySelection);
        String roomQuery = locationFilter.getText().trim().toLowerCase(Locale.ENGLISH);
        for (Section section : cachedSections) {
            if (targetDay != null && section.getDayOfWeek() != targetDay) {
                continue;
            }
            if (!roomQuery.isEmpty()) {
                String location = section.getLocation() != null ? section.getLocation().toLowerCase(Locale.ENGLISH) : "";
                if (!location.contains(roomQuery)) {
                    continue;
                }
            }
            int enrolled = (int) DatabaseUtil.getEnrollmentsForSection(section.getSectionId()).stream()
                    .filter(rec -> rec.getStatus() == EnrollmentRecord.Status.ENROLLED)
                    .count();
            sectionModel.addRow(new Object[]{
                    section.getSectionId(),
                    section.getCourseId(),
                    section.getDayOfWeek(),
                    section.getStartTime() != null ? section.getStartTime() + " - " + section.getEndTime() : "-",
                    section.getLocation(),
                    section.getCapacity(),
                    enrolled
            });
        }
    }

    private DayOfWeek resolveDay(String label) {
        if (label == null || "All".equalsIgnoreCase(label)) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(label.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String[] buildDayFilter() {
        List<String> values = new ArrayList<>();
        values.add("All");
        for (DayOfWeek day : DayOfWeek.values()) {
            values.add(capitalize(day.name()));
        }
        return values.toArray(new String[0]);
    }

    private String capitalize(String value) {
        String lower = value.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static final class SectionOption {
        private final Section section;

        SectionOption(Section section) {
            this.section = section;
        }

        @Override
        public String toString() {
            return section.getSectionId() + " - " + section.getTitle();
        }
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable callback;

        SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            callback.run();
        }
    }
}

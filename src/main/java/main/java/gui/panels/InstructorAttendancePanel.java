package main.java.gui.panels;

import main.java.models.AttendanceRecord;
import main.java.models.AttendanceRecord.AttendanceStatus;
import main.java.models.Section;
import main.java.models.Student;
import main.java.models.User;
import main.java.service.InstructorService;
import main.java.utils.DatabaseUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Enhanced attendance workspace for instructors with CSV import/export,
 * tardiness tracking, and analytics.
 */
public class InstructorAttendancePanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final User instructor;
    private final JComboBox<SectionOption> sectionCombo;
    private final JTextField dateField;
    private final DefaultTableModel tableModel;
    private final JTable attendanceTable;
    private final JButton saveButton;
    private final JButton loadButton;
    private final JButton markAllPresentButton;
    private final JButton markAllAbsentButton;
    private final JButton markAllLateButton;
    private final JButton importCsvButton;
    private final JButton exportCsvButton;
    private final JLabel sessionSummaryLabel;
    private final AttendanceAnalyticsPanel analyticsPanel;

    private List<Section> sections = List.of();
    private List<AttendanceRecord> history = List.of();

    public InstructorAttendancePanel(User instructor) {
        this.instructor = instructor;
        this.sectionCombo = new JComboBox<>();
        this.dateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 10);
        this.tableModel = new DefaultTableModel(new Object[]{"Student ID", "Name", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? AttendanceStatus.class : String.class;
            }
        };
        this.tableModel.addTableModelListener(e -> updateSessionSummary());
        this.attendanceTable = new JTable(tableModel);
        configureStatusColumn();

        this.saveButton = createPrimaryButton("Save Session", new Color(34, 197, 94));
        this.loadButton = createPrimaryButton("Load", new Color(59, 130, 246));
        this.markAllPresentButton = createPrimaryButton("All Present", new Color(37, 99, 235));
        this.markAllAbsentButton = createPrimaryButton("All Absent", new Color(220, 38, 38));
        this.markAllLateButton = createPrimaryButton("Mark Selected Late", new Color(234, 179, 8));
        this.importCsvButton = createPrimaryButton("Import CSV", new Color(107, 114, 128));
        this.exportCsvButton = createPrimaryButton("Export CSV", new Color(16, 185, 129));
        this.sessionSummaryLabel = new JLabel(" ");
        this.analyticsPanel = new AttendanceAnalyticsPanel();

        buildLayout();
        hookListeners();
        refreshSections();
    }

    private JButton createPrimaryButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void configureStatusColumn() {
        attendanceTable.setRowHeight(24);
        attendanceTable.setFillsViewportHeight(true);
        attendanceTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        TableColumn statusColumn = attendanceTable.getColumnModel().getColumn(2);
        JComboBox<AttendanceStatus> editorCombo = new JComboBox<>(AttendanceStatus.values());
        statusColumn.setCellEditor(new DefaultCellEditor(editorCombo));
        statusColumn.setCellRenderer(new StatusCellRenderer());
    }

    private void buildLayout() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Instructor Attendance Workspace");
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filters.add(new JLabel("Section:"));
        filters.add(sectionCombo);
        filters.add(new JLabel("Date (yyyy-MM-dd):"));
        filters.add(dateField);
        filters.add(loadButton);
        filters.add(saveButton);

        JPanel bulkActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bulkActions.add(markAllPresentButton);
        bulkActions.add(markAllAbsentButton);
        bulkActions.add(markAllLateButton);
        bulkActions.add(importCsvButton);
        bulkActions.add(exportCsvButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(filters, BorderLayout.CENTER);
        header.add(bulkActions, BorderLayout.SOUTH);

        JScrollPane tableScroll = new JScrollPane(attendanceTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Mark attendance and tardiness"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, analyticsPanel);
        splitPane.setResizeWeight(0.65);

        JPanel footer = new JPanel(new BorderLayout());
        sessionSummaryLabel.setForeground(new Color(30, 64, 175));
        footer.add(sessionSummaryLabel, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void hookListeners() {
        sectionCombo.addActionListener(e -> refreshData());
        loadButton.addActionListener(e -> refreshData());
        saveButton.addActionListener(e -> saveAttendance());
        markAllPresentButton.addActionListener(e -> setAllStatuses(AttendanceStatus.PRESENT));
        markAllAbsentButton.addActionListener(e -> setAllStatuses(AttendanceStatus.ABSENT));
        markAllLateButton.addActionListener(e -> markSelectedLate());
        importCsvButton.addActionListener(e -> importCsv());
        exportCsvButton.addActionListener(e -> exportCsv());
    }

    private void refreshSections() {
        sections = new ArrayList<>(InstructorService.getAssignedSections(instructor));
        sectionCombo.removeAllItems();
        for (Section section : sections) {
            sectionCombo.addItem(new SectionOption(section));
        }
        if (sectionCombo.getItemCount() > 0) {
            sectionCombo.setSelectedIndex(0);
        }
        refreshData();
    }

    private void refreshData() {
        Section section = getSelectedSection();
        if (section == null) {
            tableModel.setRowCount(0);
            history = List.of();
            analyticsPanel.updateStats(history);
            sessionSummaryLabel.setText("No sections assigned.");
            return;
        }
        history = DatabaseUtil.getAttendanceForSection(section.getSectionId());
        populateRoster(section);
        analyticsPanel.updateStats(history);
    }

    private Section getSelectedSection() {
        SectionOption option = (SectionOption) sectionCombo.getSelectedItem();
        return option != null ? option.section() : null;
    }

    private void populateRoster(Section section) {
        tableModel.setRowCount(0);
        LocalDate date = resolveDate();
        Map<String, AttendanceStatus> statuses = findStatusesForDate(section.getSectionId(), date);
        for (String studentId : section.getEnrolledStudentIds()) {
            Student student = DatabaseUtil.getStudent(studentId);
            AttendanceStatus status = statuses.getOrDefault(studentId, AttendanceStatus.PRESENT);
            tableModel.addRow(new Object[]{
                    studentId,
                    student != null ? student.getFullName() : studentId,
                    status
            });
        }
        updateSessionSummary();
    }

    private LocalDate resolveDate() {
        try {
            return LocalDate.parse(dateField.getText().trim(), DATE_FORMATTER);
        } catch (Exception ex) {
            LocalDate today = LocalDate.now();
            dateField.setText(today.format(DATE_FORMATTER));
            return today;
        }
    }

    private Map<String, AttendanceStatus> findStatusesForDate(String sectionId, LocalDate date) {
        return history.stream()
                .filter(record -> record.getSectionId().equals(sectionId) && record.getDate().equals(date))
                .findFirst()
                .map(record -> new HashMap<>(record.getStatusByStudent()))
                .orElseGet(HashMap::new);
    }

    private void saveAttendance() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "No section selected.");
            return;
        }
        if (DatabaseUtil.isMaintenanceMode()) {
            JOptionPane.showMessageDialog(this, "Cannot save during maintenance mode.", "Maintenance", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate date = resolveDate();
        Map<String, AttendanceStatus> statuses = new HashMap<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String studentId = (String) tableModel.getValueAt(row, 0);
            AttendanceStatus status = getStatusAtRow(row);
            statuses.put(studentId, status);
        }
        DatabaseUtil.recordAttendance(section.getSectionId(), date, statuses);
        JOptionPane.showMessageDialog(this, "Attendance saved for " + section.getSectionId() + " on " + date + ".");
        refreshData();
    }

    private void setAllStatuses(AttendanceStatus status) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            tableModel.setValueAt(status, row, 2);
        }
        updateSessionSummary();
    }

    private void markSelectedLate() {
        int[] selectedRows = attendanceTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select at least one student to mark as late.");
            return;
        }
        for (int viewRow : selectedRows) {
            int modelRow = attendanceTable.convertRowIndexToModel(viewRow);
            tableModel.setValueAt(AttendanceStatus.LATE, modelRow, 2);
        }
        updateSessionSummary();
    }

    private AttendanceStatus getStatusAtRow(int row) {
        Object value = tableModel.getValueAt(row, 2);
        if (value instanceof AttendanceStatus status) {
            return status;
        }
        if (value instanceof String text) {
            return parseStatus(text);
        }
        return AttendanceStatus.PRESENT;
    }

    private void updateSessionSummary() {
        int total = tableModel.getRowCount();
        long present = 0;
        long late = 0;
        long absent = 0;
        for (int row = 0; row < total; row++) {
            AttendanceStatus status = getStatusAtRow(row);
            if (status == AttendanceStatus.LATE) {
                late++;
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
            } else {
                present++;
            }
        }
        sessionSummaryLabel.setText(String.format("Present %d / %d • Late %d • Absent %d",
                present, total, late, absent));
    }

    private void importCsv() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Select a section before importing.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Map<LocalDate, Map<String, AttendanceStatus>> payload = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(chooser.getSelectedFile().toPath())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);
            for (CSVRecord record : records) {
                String dateValue = record.get("Date");
                String studentId = record.get("Student ID").trim();
                String statusValue = record.get("Status");
                LocalDate date = LocalDate.parse(dateValue.trim(), DATE_FORMATTER);
                if (!section.getEnrolledStudentIds().contains(studentId)) {
                    continue;
                }
                AttendanceStatus status = parseStatus(statusValue);
                payload.computeIfAbsent(date, d -> new HashMap<>()).put(studentId, status);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to import CSV: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (payload.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No matching rows found for this section.");
            return;
        }
        payload.forEach((date, statuses) -> DatabaseUtil.recordAttendance(section.getSectionId(), date, statuses));
        JOptionPane.showMessageDialog(this, "Imported " + payload.size() + " sessions.");
        refreshData();
    }

    private void exportCsv() {
        Section section = getSelectedSection();
        if (section == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No attendance history to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(section.getSectionId() + "_attendance.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(chooser.getSelectedFile()),
                CSVFormat.DEFAULT.withHeader("Date", "Student ID", "Status"))) {
            for (AttendanceRecord record : history) {
                for (Map.Entry<String, AttendanceStatus> entry : record.getStatusByStudent().entrySet()) {
                    printer.printRecord(
                            record.getDate().format(DATE_FORMATTER),
                            entry.getKey(),
                            entry.getValue().name());
                }
            }
            JOptionPane.showMessageDialog(this, "Attendance exported.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static AttendanceStatus parseStatus(String raw) {
        if (raw == null) {
            return AttendanceStatus.PRESENT;
        }
        String normalized = raw.trim().toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case "ABSENT", "A", "0" -> AttendanceStatus.ABSENT;
            case "LATE", "L", "TARDY" -> AttendanceStatus.LATE;
            default -> AttendanceStatus.PRESENT;
        };
    }

    private static class SectionOption {
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

    private final class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            AttendanceStatus status = value instanceof AttendanceStatus
                    ? (AttendanceStatus) value
                    : parseStatus(Objects.toString(value, "PRESENT"));
            setText(status.name().substring(0, 1) + status.name().substring(1).toLowerCase(Locale.ENGLISH));
            if (!isSelected) {
                switch (status) {
                    case ABSENT -> setForeground(new Color(220, 38, 38));
                    case LATE -> setForeground(new Color(234, 179, 8));
                    default -> setForeground(new Color(34, 197, 94));
                }
            } else {
                setForeground(Color.WHITE);
            }
            return c;
        }
    }

    private final class AttendanceAnalyticsPanel extends JPanel {
        private final JLabel avgLabel = createValueLabel();
        private final JLabel tardyLabel = createValueLabel();
        private final JLabel sessionsLabel = createValueLabel();
        private final HistoryChart chart = new HistoryChart();

        AttendanceAnalyticsPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Attendance analytics"),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            JPanel metrics = new JPanel(new GridLayout(3, 1, 8, 8));
            metrics.add(buildMetric("Average attendance", avgLabel));
            metrics.add(buildMetric("Tardy incidents", tardyLabel));
            metrics.add(buildMetric("Sessions tracked", sessionsLabel));
            add(metrics, BorderLayout.NORTH);
            add(chart, BorderLayout.CENTER);
        }

        private JLabel createValueLabel() {
            JLabel label = new JLabel("—");
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setForeground(new Color(30, 41, 59));
            return label;
        }

        private JPanel buildMetric(String title, JLabel valueLabel) {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel heading = new JLabel(title.toUpperCase(Locale.ENGLISH));
            heading.setFont(new Font("Arial", Font.BOLD, 11));
            heading.setForeground(new Color(100, 116, 139));
            panel.add(heading, BorderLayout.NORTH);
            panel.add(valueLabel, BorderLayout.CENTER);
            return panel;
        }

        void updateStats(List<AttendanceRecord> history) {
            if (history == null || history.isEmpty()) {
                avgLabel.setText("—");
                tardyLabel.setText("—");
                sessionsLabel.setText("0");
                chart.setHistory(Collections.emptyList());
                return;
            }
            double average = history.stream()
                    .mapToDouble(AttendanceRecord::getAttendancePercentage)
                    .average()
                    .orElse(0.0);
            long tardies = history.stream()
                    .mapToLong(AttendanceRecord::getTardyCount)
                    .sum();
            avgLabel.setText(String.format(Locale.ENGLISH, "%.1f%%", average));
            tardyLabel.setText(String.valueOf(tardies));
            sessionsLabel.setText(String.valueOf(history.size()));
            chart.setHistory(history);
        }
    }

    private static final class HistoryChart extends JPanel {
        private List<AttendanceRecord> history = List.of();

        HistoryChart() {
            setPreferredSize(new Dimension(260, 220));
        }

        void setHistory(List<AttendanceRecord> history) {
            if (history == null) {
                this.history = List.of();
            } else {
                this.history = history.size() > 12
                        ? history.subList(history.size() - 12, history.size())
                        : history;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, width, height);
            if (history.isEmpty()) {
                g2.setColor(new Color(148, 163, 184));
                g2.drawString("No sessions", 10, height / 2);
                g2.dispose();
                return;
            }
            int barWidth = Math.max(12, (width - 30) / history.size());
            int x = 20;
            for (AttendanceRecord record : history) {
                double percentage = record.getAttendancePercentage() / 100.0;
                int barHeight = (int) ((height - 40) * percentage);
                g2.setColor(new Color(59, 130, 246));
                g2.fillRoundRect(x, height - barHeight - 20, barWidth, barHeight, 6, 6);
                g2.setColor(new Color(30, 41, 59));
                g2.setFont(g2.getFont().deriveFont(10f));
                String label = record.getDate().format(DateTimeFormatter.ofPattern("MM/dd"));
                g2.drawString(label, x, height - 5);
                x += barWidth + 6;
            }
            g2.dispose();
        }
    }
}

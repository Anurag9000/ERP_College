package main.java.gui.panels;

import main.java.utils.AuditLogService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Administrative view for security audit events with CSV export.
 */
public class AuditLogPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JComboBox<String> rangeCombo;
    private final DefaultTableModel tableModel;
    private List<AuditLogService.AuditEvent> currentEvents;

    public AuditLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Audit Trail");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        rangeCombo = new JComboBox<>(new String[]{
                "Last 24 Hours",
                "Last 7 Days",
                "Last 30 Days",
                "All (latest 250)"
        });

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadEvents());

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportCsv());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(new JLabel("Range:"));
        controls.add(rangeCombo);
        controls.add(refreshButton);
        controls.add(exportButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Timestamp", "Type", "Actor", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(480);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadEvents();
    }

    private void loadEvents() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from;
        switch ((String) rangeCombo.getSelectedItem()) {
            case "Last 24 Hours" -> from = now.minusHours(24);
            case "Last 7 Days" -> from = now.minusDays(7);
            case "Last 30 Days" -> from = now.minusDays(30);
            default -> from = null;
        }
        currentEvents = from == null
                ? AuditLogService.recentEvents()
                : AuditLogService.findBetween(from, now);

        tableModel.setRowCount(0);
        for (AuditLogService.AuditEvent event : currentEvents) {
            tableModel.addRow(new Object[]{
                    FORMATTER.format(event.getTimestamp()),
                    event.getType().name(),
                    event.getActor(),
                    event.getDetails()
            });
        }
    }

    private void exportCsv() {
        if (currentEvents == null || currentEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No audit events to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Audit Trail");
        chooser.setSelectedFile(new java.io.File("audit-log.csv"));
        int option = chooser.showSaveDialog(this);
        if (option != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path target = chooser.getSelectedFile().toPath();
        if (Files.exists(target)) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "File exists. Overwrite?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            AuditLogService.exportToCsv(target, currentEvents);
            JOptionPane.showMessageDialog(this, "Audit log exported to " + target.toAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to export audit log: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

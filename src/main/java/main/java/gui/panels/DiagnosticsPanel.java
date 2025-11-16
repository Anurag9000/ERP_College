package main.java.gui.panels;

import main.java.utils.HealthDiagnostics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Admin-facing diagnostics panel that runs datasource/config checks.
 */
public class DiagnosticsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JButton runButton;

    public DiagnosticsPanel() {
        this.tableModel = new DefaultTableModel(new Object[]{"Check", "Status", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.runButton = new JButton("Run Diagnostics");

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("System Diagnostics");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(runButton);
        add(controls, BorderLayout.SOUTH);

        runButton.addActionListener(e -> runDiagnostics());
        runDiagnostics();
    }

    private void runDiagnostics() {
        runButton.setEnabled(false);
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (HealthDiagnostics.CheckResult result : HealthDiagnostics.runAll()) {
                tableModel.addRow(new Object[]{
                        result.getName(),
                        result.isSuccess() ? "OK" : "FAIL",
                        result.getDetails()
                });
            }
            runButton.setEnabled(true);
        });
    }
}

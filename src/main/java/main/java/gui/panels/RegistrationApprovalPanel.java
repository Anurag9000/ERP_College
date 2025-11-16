package main.java.gui.panels;

import main.java.models.User;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Admin panel to approve/reject advisor-required registration requests.
 */
public class RegistrationApprovalPanel extends JPanel {
    private final User adminUser;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton approveButton;
    private final JButton rejectButton;
    private final JButton refreshButton;

    public RegistrationApprovalPanel(User adminUser) {
        this.adminUser = adminUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Registration Approvals");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{
                "ID", "Student", "Section", "Requested By", "Requested At"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        approveButton = new JButton("Approve");
        rejectButton = new JButton("Reject");
        refreshButton = new JButton("Refresh");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(approveButton);
        controls.add(rejectButton);
        controls.add(refreshButton);
        add(controls, BorderLayout.SOUTH);

        approveButton.addActionListener(e -> processRequest(true));
        rejectButton.addActionListener(e -> processRequest(false));
        refreshButton.addActionListener(e -> loadData());

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (DatabaseUtil.RegistrationRequestView view : DatabaseUtil.getPendingRegistrationRequests()) {
            tableModel.addRow(new Object[]{
                    view.id(),
                    view.studentName() + " (" + view.studentId() + ")",
                    view.sectionTitle() + " (" + view.sectionId() + ")",
                    view.requestedBy(),
                    view.requestedAt()
            });
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = table.getSelectedRow() != -1;
        approveButton.setEnabled(hasSelection);
        rejectButton.setEnabled(hasSelection);
    }

    private void processRequest(boolean approve) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a request first.");
            return;
        }
        long id = ((Number) tableModel.getValueAt(row, 0)).longValue();
        String notes = approve ? null : JOptionPane.showInputDialog(this, "Optional rejection notes:");
        try {
            if (approve) {
                DatabaseUtil.approveRegistrationRequest(adminUser, id, notes);
                JOptionPane.showMessageDialog(this, "Registration approved.");
            } else {
                DatabaseUtil.rejectRegistrationRequest(adminUser, id, notes);
                JOptionPane.showMessageDialog(this, "Registration rejected.");
            }
            loadData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

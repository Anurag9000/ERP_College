package main.java.gui.panels;

import main.java.models.Section;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * Admin panel to review waitlist entries and approve/revoke advisor approval.
 */
public class WaitlistApprovalPanel extends JPanel {
    private final User adminUser;
    private final JComboBox<String> sectionCombo;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton approveButton;
    private final JButton revokeButton;
    private final JButton refreshButton;

    public WaitlistApprovalPanel(User adminUser) {
        this.adminUser = Objects.requireNonNull(adminUser, "adminUser");
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Waitlist Advisor Approvals");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        sectionCombo = new JComboBox<>();
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selector.add(new JLabel("Section:"));
        selector.add(sectionCombo);
        add(selector, BorderLayout.BEFORE_FIRST_LINE);

        tableModel = new DefaultTableModel(new Object[]{"Position", "Student ID", "Name", "Approved"}, 0) {
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
        revokeButton = new JButton("Revoke");
        refreshButton = new JButton("Refresh");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(approveButton);
        controls.add(revokeButton);
        controls.add(refreshButton);
        add(controls, BorderLayout.SOUTH);

        sectionCombo.addActionListener(e -> refreshTable());
        approveButton.addActionListener(e -> modifyApproval(true));
        revokeButton.addActionListener(e -> modifyApproval(false));
        refreshButton.addActionListener(e -> refreshTable());

        loadSections();
        refreshTable();
    }

    private void loadSections() {
        sectionCombo.removeAllItems();
        for (Section section : DatabaseUtil.getAllSections()) {
            sectionCombo.addItem(section.getSectionId() + " - " + section.getTitle());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String sectionSelection = (String) sectionCombo.getSelectedItem();
        if (sectionSelection == null) {
            approveButton.setEnabled(false);
            revokeButton.setEnabled(false);
            return;
        }
        String sectionId = sectionSelection.split(" - ")[0];
        List<DatabaseUtil.WaitlistSnapshot> entries = DatabaseUtil.getWaitlistSnapshot(sectionId);
        for (DatabaseUtil.WaitlistSnapshot entry : entries) {
            tableModel.addRow(new Object[]{
                    entry.position(),
                    entry.studentId(),
                    entry.studentName(),
                    entry.approved() ? "Yes" : "Pending"
            });
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = table.getSelectedRow() != -1;
        approveButton.setEnabled(hasSelection);
        revokeButton.setEnabled(hasSelection);
    }

    private void modifyApproval(boolean approved) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a waitlist entry first.");
            return;
        }
        String sectionSelection = (String) sectionCombo.getSelectedItem();
        if (sectionSelection == null) {
            return;
        }
        String sectionId = sectionSelection.split(" - ")[0];
        String studentId = (String) tableModel.getValueAt(row, 1);
        try {
            DatabaseUtil.setWaitlistApproval(adminUser, sectionId, studentId, approved);
            String status = approved ? "approved" : "revoked";
            JOptionPane.showMessageDialog(this, "Advisor approval " + status + " for " + studentId + ".");
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update approval", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package main.java.gui.panels;

import main.java.models.FeeInstallment;
import main.java.models.PaymentTransaction;
import main.java.models.Student;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Panel for managing student finance operations.
 */
public class FeesPanel extends JPanel implements MaintenanceAware {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JTable feesTable;
    private final DefaultTableModel tableModel;
    private final JTextField searchField;
    private final JButton paymentButton;
    private final JButton exportStatementButton;
    private final JButton exportSummaryButton;
    private final JButton configureInstallmentsButton;
    private final JButton refreshButton;
    private final JLabel totalOutstandingLabel;
    private boolean maintenanceMode;

    public FeesPanel() {
        this.tableModel = new DefaultTableModel(new Object[]{
                "Student ID", "Name", "Course", "Total Fees",
                "Fees Paid", "Outstanding", "Status", "Next Due"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.feesTable = new JTable(tableModel);
        this.searchField = new JTextField(20);
        this.paymentButton = new JButton("Record Payment");
        this.exportStatementButton = new JButton("Export Statement");
        this.exportSummaryButton = new JButton("Export Summary");
        this.configureInstallmentsButton = new JButton("Configure Installments");
        this.refreshButton = new JButton("Refresh");
        this.totalOutstandingLabel = new JLabel();

        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadFeesData();
    }

    private void initializeComponents() {
        feesTable.setRowHeight(24);
        feesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        feesTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        feesTable.setRowSorter(sorter);

        searchField.setToolTipText("Search students...");

        stylePrimaryButton(paymentButton, new Color(34, 197, 94));
        stylePrimaryButton(exportStatementButton, new Color(37, 99, 235));
        stylePrimaryButton(exportSummaryButton, new Color(59, 130, 246));
        stylePrimaryButton(configureInstallmentsButton, new Color(249, 115, 22));
        stylePrimaryButton(refreshButton, new Color(107, 114, 128));

        paymentButton.setEnabled(false);
        exportStatementButton.setEnabled(false);
        configureInstallmentsButton.setEnabled(false);

        totalOutstandingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalOutstandingLabel.setForeground(new Color(220, 38, 38));
    }

    private void stylePrimaryButton(AbstractButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Fee Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(paymentButton);
        controls.add(configureInstallmentsButton);
        controls.add(exportStatementButton);
        controls.add(exportSummaryButton);
        controls.add(refreshButton);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.add(totalOutstandingLabel);

        JPanel controlRow = new JPanel(new BorderLayout());
        controlRow.add(controls, BorderLayout.WEST);
        controlRow.add(summaryPanel, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.add(headerPanel, BorderLayout.NORTH);
        top.add(controlRow, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(feesTable), BorderLayout.CENTER);
    }

    private void setupEventHandlers() {
        feesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });

        DocumentListener searchListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        };
        searchField.getDocument().addDocumentListener(searchListener);

        paymentButton.addActionListener(e -> recordPayment());
        configureInstallmentsButton.addActionListener(e -> openInstallmentDialog());
        exportStatementButton.addActionListener(e -> exportStatement());
        exportSummaryButton.addActionListener(e -> exportSummary());
        refreshButton.addActionListener(e -> loadFeesData());
    }

    private void loadFeesData() {
        tableModel.setRowCount(0);
        Collection<Student> students = DatabaseUtil.getAllStudents();
        double totalOutstanding = 0.0;

        for (Student student : students) {
            double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
            totalOutstanding += outstanding;
            FeeInstallment next = DatabaseUtil.nextDueInstallment(student.getStudentId());
            String nextDue = "-";
            if (next != null && next.getDueDate() != null) {
                nextDue = DATE_FORMATTER.format(next.getDueDate());
                if (next.isOverdue(LocalDate.now())) {
                    nextDue += " (Overdue)";
                }
            } else if (student.getNextFeeDueDate() != null) {
                nextDue = DATE_FORMATTER.format(student.getNextFeeDueDate());
            }
            String status = outstanding > 0 ? "Pending" : "Settled";

            tableModel.addRow(new Object[]{
                    student.getStudentId(),
                    student.getFullName(),
                    student.getCourse(),
                    formatCurrency(student.getTotalFees()),
                    formatCurrency(student.getFeesPaid()),
                    formatCurrency(outstanding),
                    status,
                    nextDue
            });
        }

        totalOutstandingLabel.setText("Total Outstanding: " + formatCurrency(totalOutstanding));
        updateActionButtons();
    }

    private void filterTable() {
        String query = searchField.getText().trim();
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) feesTable.getRowSorter();
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void recordPayment() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        Student student = getSelectedStudent();
        if (student == null) {
            return;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField amountField = new JTextField();
        JTextField methodField = new JTextField("Cash");
        JTextField referenceField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        panel.add(new JLabel("Outstanding:"));
        panel.add(new JLabel(formatCurrency(Math.max(0.0, student.getTotalFees() - student.getFeesPaid()))));
        panel.add(new JLabel("Amount:"));
        panel.add(amountField);
        panel.add(new JLabel("Method:"));
        panel.add(methodField);
        panel.add(new JLabel("Reference:"));
        panel.add(referenceField);
        panel.add(new JLabel("Notes:"));
        panel.add(new JScrollPane(notesArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "Record Payment", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
            if (amount <= 0 || amount > outstanding) {
                JOptionPane.showMessageDialog(this, "Enter an amount between 0 and " + formatCurrency(outstanding),
                        "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseUtil.recordPayment("finance-panel", student.getStudentId(), amount,
                    methodField.getText().trim(),
                    referenceField.getText().trim(),
                    notesArea.getText().trim());
            JOptionPane.showMessageDialog(this, "Payment recorded successfully.");
            loadFeesData();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInstallmentDialog() {
        if (maintenanceMode) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return;
        }
        Student student = getSelectedStudent();
        if (student == null) {
            return;
        }
        InstallmentEditorDialog dialog = new InstallmentEditorDialog(SwingUtilities.getWindowAncestor(this), student);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadFeesData();
        }
    }

    private void exportSummary() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("finance_summary.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(chooser.getSelectedFile()),
                CSVFormat.DEFAULT.withHeader("Student ID", "Name", "Course", "Total Fees", "Fees Paid", "Outstanding", "Status", "Next Due"))) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                printer.printRecord(
                        tableModel.getValueAt(row, 0),
                        tableModel.getValueAt(row, 1),
                        tableModel.getValueAt(row, 2),
                        tableModel.getValueAt(row, 3),
                        tableModel.getValueAt(row, 4),
                        tableModel.getValueAt(row, 5),
                        tableModel.getValueAt(row, 6),
                        tableModel.getValueAt(row, 7)
                );
            }
            JOptionPane.showMessageDialog(this, "Summary exported successfully.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export summary: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportStatement() {
        Student student = getSelectedStudent();
        if (student == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(student.getStudentId() + "_statement.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<PaymentTransaction> transactions = DatabaseUtil.getPaymentHistoryForStudent(student.getStudentId());
        List<FeeInstallment> installments = DatabaseUtil.getInstallmentsForStudent(student.getStudentId());

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(chooser.getSelectedFile()),
                CSVFormat.DEFAULT)) {
            printer.printRecord("Student", student.getFullName());
            printer.printRecord("Program", student.getCourse());
            printer.printRecord("Total Fees", formatCurrency(student.getTotalFees()));
            printer.printRecord("Fees Paid", formatCurrency(student.getFeesPaid()));
            printer.printRecord("Outstanding", formatCurrency(Math.max(0.0, student.getTotalFees() - student.getFeesPaid())));
            printer.println();

            printer.printRecord("Payments");
            printer.printRecord("Date", "Amount", "Method", "Reference", "Notes");
            for (PaymentTransaction tx : transactions) {
                printer.printRecord(
                        DATE_FORMATTER.format(tx.getPaidOn()),
                        formatCurrency(tx.getAmount()),
                        emptyIfNull(tx.getMethod()),
                        emptyIfNull(tx.getReference()),
                        emptyIfNull(tx.getNotes())
                );
            }
            printer.println();

            printer.printRecord("Installments");
            printer.printRecord("Due Date", "Amount", "Status", "Description", "Paid On", "Last Reminder");
            for (FeeInstallment installment : installments) {
                printer.printRecord(
                        installment.getDueDate() != null ? DATE_FORMATTER.format(installment.getDueDate()) : "-",
                        formatCurrency(installment.getAmount()),
                        installment.getStatus().name(),
                        emptyIfNull(installment.getDescription()),
                        installment.getPaidOn() != null ? DATE_FORMATTER.format(installment.getPaidOn()) : "-",
                        installment.getLastReminderSent() != null ? DATE_FORMATTER.format(installment.getLastReminderSent()) : "-"
                );
            }
            JOptionPane.showMessageDialog(this, "Statement exported successfully.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export statement: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Student getSelectedStudent() {
        int viewRow = feesTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return null;
        }
        int modelRow = feesTable.convertRowIndexToModel(viewRow);
        String studentId = (String) tableModel.getValueAt(modelRow, 0);
        return DatabaseUtil.getStudent(studentId);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.ENGLISH, "\u20B9%,.0f", amount);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private final class InstallmentEditorDialog extends JDialog {
        private final Student student;
        private final DefaultTableModel model;
        private final JTable table;
        private final List<String> removedIds = new ArrayList<>();
        private boolean saved = false;

        InstallmentEditorDialog(Window owner, Student student) {
            super(owner, "Configure Installments - " + student.getFullName(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            this.student = student;
            this.model = new DefaultTableModel(new Object[]{
                    "ID", "Due Date (yyyy-MM-dd)", "Amount", "Status", "Description", "Paid On (yyyy-MM-dd)"
            }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 0;
                }
            };
            this.table = new JTable(model);
            table.setRowHeight(22);
            table.getColumnModel().getColumn(0).setPreferredWidth(160);
            populateRows();
            buildUi();
        }

        boolean isSaved() {
            return saved;
        }

        private void populateRows() {
            model.setRowCount(0);
            for (FeeInstallment installment : DatabaseUtil.getInstallmentsForStudent(student.getStudentId())) {
                model.addRow(new Object[]{
                        installment.getInstallmentId(),
                        installment.getDueDate() != null ? INPUT_DATE_FORMAT.format(installment.getDueDate()) : "",
                        installment.getAmount(),
                        installment.getStatus().name(),
                        installment.getDescription() != null ? installment.getDescription() : "",
                        installment.getPaidOn() != null ? INPUT_DATE_FORMAT.format(installment.getPaidOn()) : ""
                });
            }
        }

        private void buildUi() {
            setLayout(new BorderLayout(10, 10));
            setPreferredSize(new Dimension(800, 400));
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            JButton addButton = new JButton("Add");
            JButton removeButton = new JButton("Remove");
            JButton saveButton = new JButton("Save");
            JButton closeButton = new JButton("Close");

            addButton.addActionListener(this::handleAdd);
            removeButton.addActionListener(this::handleRemove);
            saveButton.addActionListener(this::handleSave);
            closeButton.addActionListener(e -> dispose());

            buttons.add(addButton);
            buttons.add(removeButton);
            buttons.add(saveButton);
            buttons.add(closeButton);
            add(buttons, BorderLayout.SOUTH);
            pack();
        }

        private void handleAdd(ActionEvent event) {
            model.addRow(new Object[]{
                    "", "", 0.0, FeeInstallment.Status.DUE.name(), "", ""
            });
        }

        private void handleRemove(ActionEvent event) {
            int row = table.getSelectedRow();
            if (row < 0) {
                return;
            }
            String id = (String) model.getValueAt(row, 0);
            if (id != null && !id.isBlank()) {
                removedIds.add(id);
            }
            model.removeRow(row);
        }

        private void handleSave(ActionEvent event) {
            try {
                for (String id : removedIds) {
                    DatabaseUtil.deleteInstallment(student.getStudentId(), id);
                }
                removedIds.clear();

                for (int i = 0; i < model.getRowCount(); i++) {
                    String id = valueAt(i, 0);
                    String dueRaw = valueAt(i, 1);
                    String amountRaw = valueAt(i, 2);
                    String statusRaw = valueAt(i, 3);
                    String description = valueAt(i, 4);
                    String paidOnRaw = valueAt(i, 5);

                    double amount = Double.parseDouble(amountRaw);
                    if (amount <= 0) {
                        throw new IllegalArgumentException("Amount must be positive (row " + (i + 1) + ").");
                    }
                    FeeInstallment.Status status = FeeInstallment.Status.valueOf(statusRaw.toUpperCase(Locale.ENGLISH));

                    LocalDate dueDate = dueRaw.isBlank() ? null : LocalDate.parse(dueRaw, INPUT_DATE_FORMAT);
                    LocalDate paidOn = paidOnRaw.isBlank() ? null : LocalDate.parse(paidOnRaw, INPUT_DATE_FORMAT);

                    FeeInstallment installment = new FeeInstallment(
                            id == null || id.isBlank() ? UUID.randomUUID().toString() : id,
                            student.getStudentId(),
                            dueDate,
                            amount,
                            status,
                            description,
                            paidOn,
                            null
                    );
                    DatabaseUtil.upsertInstallment(student.getStudentId(), installment);
                }

                saved = true;
                JOptionPane.showMessageDialog(this, "Installments updated successfully.");
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String valueAt(int row, int column) {
            Object value = model.getValueAt(row, column);
            return value == null ? "" : value.toString().trim();
        }
    }

    @Override
    public void onMaintenanceModeChanged(boolean maintenance) {
        this.maintenanceMode = maintenance;
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasSelection = feesTable.getSelectedRow() != -1;
        paymentButton.setEnabled(hasSelection && !maintenanceMode);
        configureInstallmentsButton.setEnabled(hasSelection && !maintenanceMode);
        exportStatementButton.setEnabled(hasSelection);
    }
}

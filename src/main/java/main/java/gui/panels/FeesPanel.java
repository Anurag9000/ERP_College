package main.java.gui.panels;

import main.java.data.dao.FeeScheduleTemplateDao;
import main.java.models.Course;
import main.java.models.FeeInstallment;
import main.java.models.PaymentTransaction;
import main.java.models.Student;
import main.java.utils.DatabaseUtil;
import main.java.gui.panels.MaintenanceAware;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Panel for managing student finance operations.
 */
public class FeesPanel extends JPanel implements MaintenanceAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeesPanel.class);
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
    private final DefaultTableModel templateModel;
    private final JTable templateTable;
    private final JComboBox<String> templateCourseSelector;
    private final JButton addTemplateButton;
    private final JButton editTemplateButton;
    private final JButton deleteTemplateButton;
    private final JButton applyTemplateButton;
    private final Map<Long, FeeScheduleTemplateDao.TemplateRecord> templateIndex;
    private boolean maintenanceMode;

    public FeesPanel() {
        this.tableModel = new DefaultTableModel(new Object[] {
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
        this.templateModel = new DefaultTableModel(new Object[] {
                "ID", "Label", "Amount", "Offset (days)", "Timeline Note"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.templateTable = new JTable(templateModel);
        this.templateCourseSelector = new JComboBox<>();
        this.addTemplateButton = new JButton("Add Template");
        this.editTemplateButton = new JButton("Edit");
        this.deleteTemplateButton = new JButton("Delete");
        this.applyTemplateButton = new JButton("Apply to Student");
        this.templateIndex = new HashMap<>();

        initializeComponents();
        refreshTemplateCourseOptions();
        loadTemplatesForSelectedCourse();
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

        templateTable.setRowHeight(24);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateTable.setAutoCreateRowSorter(true);
        templateTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        templateTable.getColumnModel().getColumn(0).setMinWidth(0);
        templateTable.getColumnModel().getColumn(0).setMaxWidth(0);
        templateTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        templateCourseSelector.setPrototypeDisplayValue("PROGRAM-0000 - Long Course Name");

        stylePrimaryButton(addTemplateButton, new Color(34, 197, 94));
        stylePrimaryButton(editTemplateButton, new Color(59, 130, 246));
        stylePrimaryButton(deleteTemplateButton, new Color(239, 68, 68));
        stylePrimaryButton(applyTemplateButton, new Color(234, 179, 8));

        editTemplateButton.setEnabled(false);
        deleteTemplateButton.setEnabled(false);
        applyTemplateButton.setEnabled(false);
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

        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Fee Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Accounts", buildStudentAccountsTab());
        tabs.addTab("Fee Templates", buildTemplateTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildStudentAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(paymentButton);
        controls.add(configureInstallmentsButton);
        controls.add(exportStatementButton);
        controls.add(exportSummaryButton);
        controls.add(refreshButton);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.add(totalOutstandingLabel);

        JPanel header = new JPanel(new BorderLayout());
        header.add(searchPanel, BorderLayout.EAST);
        header.add(controls, BorderLayout.WEST);
        header.add(summaryPanel, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(feesTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTemplateTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.add(new JLabel("Course:"));
        controls.add(templateCourseSelector);
        controls.add(addTemplateButton);
        controls.add(editTemplateButton);
        controls.add(deleteTemplateButton);
        controls.add(applyTemplateButton);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(templateTable), BorderLayout.CENTER);
        return panel;
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
        templateCourseSelector.addActionListener(e -> loadTemplatesForSelectedCourse());
        templateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateTemplateActions();
            }
        });
        addTemplateButton.addActionListener(e -> handleAddTemplate());
        editTemplateButton.addActionListener(e -> handleEditTemplate());
        deleteTemplateButton.addActionListener(e -> handleDeleteTemplate());
        applyTemplateButton.addActionListener(e -> handleApplyTemplate());
    }

    private void loadFeesData() {
        refreshButton.setEnabled(false);
        tableModel.setRowCount(0);

        new SwingWorker<List<Object[]>, Void>() {
            private double totalOutstandingValue = 0.0;

            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<>();
                Collection<Student> students = DatabaseUtil.getAllStudents();
                for (Student student : students) {
                    double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
                    totalOutstandingValue += outstanding;

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

                    rows.add(new Object[] {
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
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                    totalOutstandingLabel.setText("Total Outstanding: " + formatCurrency(totalOutstandingValue));
                } catch (Exception ex) {
                    LOGGER.error("Failed to load fees data", ex);
                    JOptionPane.showMessageDialog(FeesPanel.this, "Error loading fees data: " + ex.getMessage());
                } finally {
                    refreshButton.setEnabled(true);
                    updateActionButtons();
                }
            }
        }.execute();
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

    private void refreshTemplateCourseOptions() {
        templateCourseSelector.removeAllItems();
        templateCourseSelector.addItem("-- Select Course --");
        List<Course> courseList = new ArrayList<>(DatabaseUtil.getAllCourses());
        courseList.sort(Comparator.comparing(course -> {
            String code = course.getCourseId();
            return code == null ? "" : code.toLowerCase(Locale.ENGLISH);
        }));
        for (Course course : courseList) {
            String code = course.getCourseId();
            if (code == null || code.isBlank()) {
                continue;
            }
            templateCourseSelector.addItem(code);
        }
        templateCourseSelector.setSelectedIndex(0);
    }

    private void loadTemplatesForSelectedCourse() {
        templateModel.setRowCount(0);
        templateIndex.clear();
        String courseCode = resolveSelectedCourseCode();
        if (courseCode == null) {
            updateTemplateActions();
            return;
        }
        List<FeeScheduleTemplateDao.TemplateRecord> records = DatabaseUtil.getFeeScheduleTemplates(courseCode);
        for (FeeScheduleTemplateDao.TemplateRecord record : records) {
            templateIndex.put(record.id(), record);
            templateModel.addRow(new Object[] {
                    record.id(),
                    record.label(),
                    formatCurrency(record.amount()),
                    record.offsetDays(),
                    describeOffset(record.offsetDays())
            });
        }
        updateTemplateActions();
    }

    private String resolveSelectedCourseCode() {
        Object selected = templateCourseSelector.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String value = selected.toString();
        if ("-- Select Course --".equals(value)) {
            return null;
        }
        return value.isBlank() ? null : value;
    }

    private void updateTemplateActions() {
        boolean hasCourse = resolveSelectedCourseCode() != null;
        boolean hasSelection = templateTable.getSelectedRow() != -1;
        boolean hasTemplates = templateModel.getRowCount() > 0;
        boolean maintenance = isMaintenanceLocked();
        addTemplateButton.setEnabled(hasCourse && !maintenance);
        editTemplateButton.setEnabled(hasCourse && hasSelection && !maintenance);
        deleteTemplateButton.setEnabled(hasCourse && hasSelection && !maintenance);
        applyTemplateButton.setEnabled(hasCourse && hasTemplates && !maintenance);
    }

    private void handleAddTemplate() {
        if (blockIfMaintenance()) {
            return;
        }
        String courseCode = resolveSelectedCourseCode();
        if (courseCode == null) {
            JOptionPane.showMessageDialog(this, "Select a course first.");
            return;
        }
        TemplateEditorDialog dialog = new TemplateEditorDialog(
                SwingUtilities.getWindowAncestor(this),
                "New Fee Template",
                null);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (!dialog.isSaved()) {
            return;
        }
        try {
            DatabaseUtil.addFeeScheduleTemplate(courseCode,
                    dialog.getLabelValue(),
                    dialog.getAmountValue(),
                    dialog.getOffsetDaysValue());
            loadTemplatesForSelectedCourse();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to save template", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditTemplate() {
        if (blockIfMaintenance()) {
            return;
        }
        FeeScheduleTemplateDao.TemplateRecord record = getSelectedTemplate();
        if (record == null) {
            JOptionPane.showMessageDialog(this, "Select a template first.");
            return;
        }
        TemplateEditorDialog dialog = new TemplateEditorDialog(
                SwingUtilities.getWindowAncestor(this),
                "Edit Fee Template",
                record);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (!dialog.isSaved()) {
            return;
        }
        try {
            DatabaseUtil.updateFeeScheduleTemplate(record.id(),
                    resolveSelectedCourseCode(),
                    dialog.getLabelValue(),
                    dialog.getAmountValue(),
                    dialog.getOffsetDaysValue());
            loadTemplatesForSelectedCourse();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to update template",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteTemplate() {
        if (blockIfMaintenance()) {
            return;
        }
        FeeScheduleTemplateDao.TemplateRecord record = getSelectedTemplate();
        if (record == null) {
            JOptionPane.showMessageDialog(this, "Select a template first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete template \"" + record.label() + "\"?",
                "Delete Template",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            DatabaseUtil.deleteFeeScheduleTemplate(record.id());
            loadTemplatesForSelectedCourse();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to delete template",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleApplyTemplate() {
        if (blockIfMaintenance()) {
            return;
        }
        String courseCode = resolveSelectedCourseCode();
        if (courseCode == null) {
            JOptionPane.showMessageDialog(this, "Select a course first.");
            return;
        }
        if (templateModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Add at least one template installment first.");
            return;
        }
        List<Student> eligible = new ArrayList<>();
        for (Student student : DatabaseUtil.getAllStudents()) {
            if (student.getCourse() != null && student.getCourse().equalsIgnoreCase(courseCode)) {
                eligible.add(student);
            }
        }
        if (eligible.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students found in " + courseCode + " to apply this template.");
            return;
        }
        eligible.sort(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER));

        JComboBox<StudentOption> studentCombo = new JComboBox<>();
        for (Student student : eligible) {
            studentCombo.addItem(new StudentOption(student.getStudentId(), student.getFullName()));
        }
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Select student to receive this schedule:"), BorderLayout.NORTH);
        panel.add(studentCombo, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Apply Template to Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        StudentOption selected = (StudentOption) studentCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            DatabaseUtil.applyFeeTemplateToStudent(courseCode, selected.id());
            JOptionPane.showMessageDialog(this,
                    "Installment plan applied to " + selected.label() + " (" + selected.id() + ").");
            loadFeesData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to apply template", JOptionPane.ERROR_MESSAGE);
        }
    }

    private FeeScheduleTemplateDao.TemplateRecord getSelectedTemplate() {
        int viewRow = templateTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = templateTable.convertRowIndexToModel(viewRow);
        Object value = templateModel.getValueAt(modelRow, 0);
        if (!(value instanceof Number)) {
            return null;
        }
        long templateId = ((Number) value).longValue();
        return templateIndex.get(templateId);
    }

    private void recordPayment() {
        if (blockIfMaintenance()) {
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

        double amountTemp;
        try {
            amountTemp = Double.parseDouble(amountField.getText().trim());
            double outstanding = Math.max(0.0, student.getTotalFees() - student.getFeesPaid());
            if (amountTemp <= 0 || amountTemp > outstanding) {
                JOptionPane.showMessageDialog(this, "Enter an amount between 0 and " + formatCurrency(outstanding),
                        "Invalid Amount", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final double amount = amountTemp;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatabaseUtil.recordPayment("finance-panel", student.getStudentId(), amount,
                        methodField.getText().trim(),
                        referenceField.getText().trim(),
                        notesArea.getText().trim());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(FeesPanel.this, "Payment recorded successfully.");
                    loadFeesData();
                } catch (Exception ex) {
                    LOGGER.error("Payment failed", ex);
                    JOptionPane.showMessageDialog(FeesPanel.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openInstallmentDialog() {
        if (blockIfMaintenance()) {
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
                CSVFormat.DEFAULT.withHeader("Student ID", "Name", "Course", "Total Fees", "Fees Paid", "Outstanding",
                        "Status", "Next Due"))) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                printer.printRecord(
                        tableModel.getValueAt(row, 0),
                        tableModel.getValueAt(row, 1),
                        tableModel.getValueAt(row, 2),
                        tableModel.getValueAt(row, 3),
                        tableModel.getValueAt(row, 4),
                        tableModel.getValueAt(row, 5),
                        tableModel.getValueAt(row, 6),
                        tableModel.getValueAt(row, 7));
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
            printer.printRecord("Outstanding",
                    formatCurrency(Math.max(0.0, student.getTotalFees() - student.getFeesPaid())));
            printer.println();

            printer.printRecord("Payments");
            printer.printRecord("Date", "Amount", "Method", "Reference", "Notes");
            for (PaymentTransaction tx : transactions) {
                printer.printRecord(
                        DATE_FORMATTER.format(tx.getPaidOn()),
                        formatCurrency(tx.getAmount()),
                        emptyIfNull(tx.getMethod()),
                        emptyIfNull(tx.getReference()),
                        emptyIfNull(tx.getNotes()));
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
                        installment.getLastReminderSent() != null
                                ? DATE_FORMATTER.format(installment.getLastReminderSent())
                                : "-");
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

    private String describeOffset(int offsetDays) {
        if (offsetDays <= 0) {
            return "Due on enrollment";
        }
        if (offsetDays % 30 == 0) {
            int months = offsetDays / 30;
            return "Due ~" + months + (months == 1 ? " month later" : " months later");
        }
        if (offsetDays % 7 == 0) {
            int weeks = offsetDays / 7;
            return "Due " + weeks + (weeks == 1 ? " week later" : " weeks later");
        }
        return "Due in " + offsetDays + " days";
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private final class TemplateEditorDialog extends JDialog {
        private final JTextField labelField;
        private final JTextField amountField;
        private final JSpinner offsetSpinner;
        private boolean saved = false;
        private String labelResult;
        private double amountResult;
        private int offsetResult;

        TemplateEditorDialog(Window owner, String title, FeeScheduleTemplateDao.TemplateRecord record) {
            super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            this.labelField = new JTextField(record != null ? record.label() : "", 20);
            this.amountField = new JTextField(
                    record != null ? String.format(Locale.ENGLISH, "%.2f", record.amount()) : "", 12);
            this.offsetSpinner = new JSpinner(new SpinnerNumberModel(
                    record != null ? record.offsetDays() : 0,
                    0,
                    1825,
                    15));
            buildUi();
        }

        boolean isSaved() {
            return saved;
        }

        String getLabelValue() {
            return labelResult;
        }

        double getAmountValue() {
            return amountResult;
        }

        int getOffsetDaysValue() {
            return offsetResult;
        }

        private void buildUi() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Label:"));
            form.add(labelField);
            form.add(new JLabel("Amount (\u20B9):"));
            form.add(amountField);
            form.add(new JLabel("Offset (days):"));
            form.add(offsetSpinner);
            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");
            buttons.add(saveButton);
            buttons.add(cancelButton);
            add(buttons, BorderLayout.SOUTH);

            saveButton.addActionListener(this::handleSave);
            cancelButton.addActionListener(e -> dispose());

            pack();
            setResizable(false);
        }

        private void handleSave(ActionEvent event) {
            String label = labelField.getText().trim();
            if (label.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Label is required.");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric amount.");
                return;
            }
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be positive.");
                return;
            }
            int offset = ((Number) offsetSpinner.getValue()).intValue();
            if (offset < 0) {
                JOptionPane.showMessageDialog(this, "Offset cannot be negative.");
                return;
            }
            this.labelResult = label;
            this.amountResult = amount;
            this.offsetResult = offset;
            this.saved = true;
            dispose();
        }
    }

    private record StudentOption(String id, String label) {
        @Override
        public String toString() {
            return label + " (" + id + ")";
        }
    }

    private final class InstallmentEditorDialog extends JDialog {
        private final Student student;
        private final DefaultTableModel model;
        private final JTable table;
        private final List<String> removedIds = new ArrayList<>();
        private boolean saved = false;

        InstallmentEditorDialog(Window owner, Student student) {
            super(owner, "Configure Installments - " + student.getFullName(),
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            this.student = student;
            this.model = new DefaultTableModel(new Object[] {
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
                model.addRow(new Object[] {
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
            model.addRow(new Object[] {
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
                            null);
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
        updateTemplateActions();
    }

    private void updateActionButtons() {
        boolean hasSelection = feesTable.getSelectedRow() != -1;
        boolean maintenance = isMaintenanceLocked();
        paymentButton.setEnabled(hasSelection && !maintenance);
        configureInstallmentsButton.setEnabled(hasSelection && !maintenance);
        exportStatementButton.setEnabled(hasSelection);
    }

    private boolean blockIfMaintenance() {
        if (isMaintenanceLocked()) {
            JOptionPane.showMessageDialog(this, "Changes are disabled during maintenance mode.");
            return true;
        }
        return false;
    }

    private boolean isMaintenanceLocked() {
        boolean maintenance = maintenanceMode || DatabaseUtil.isMaintenanceMode();
        if (maintenanceMode != maintenance) {
            maintenanceMode = maintenance;
        }
        return maintenance;
    }
}

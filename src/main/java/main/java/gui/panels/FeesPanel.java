package main.java.gui.panels;

import main.java.models.Student;
import main.java.utils.DatabaseUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collection;
import javax.swing.RowFilter;

/**
 * Panel for managing student fees and payments
 */
public class FeesPanel extends JPanel {
    private JTable feesTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton paymentButton, receiptButton, refreshButton;
    private JLabel totalOutstandingLabel;
    
    private final String[] columnNames = {
        "Student ID", "Name", "Course", "Total Fees", 
        "Fees Paid", "Outstanding", "Status"
    };
    
    public FeesPanel() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadFeesData();
    }
    
    private void initializeComponents() {
        // Table
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        feesTable = new JTable(tableModel);
        feesTable.setRowHeight(25);
        feesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        feesTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        feesTable.setRowSorter(sorter);
        
        // Search field
        searchField = new JTextField(20);
        searchField.setToolTipText("Search students...");
        
        // Buttons
        paymentButton = new JButton("Record Payment");
        receiptButton = new JButton("Generate Receipt");
        refreshButton = new JButton("Refresh");
        
        // Labels
        totalOutstandingLabel = new JLabel("Total Outstanding: ₹0");
        totalOutstandingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalOutstandingLabel.setForeground(new Color(220, 38, 38));
        
        // Style buttons
        Color primaryColor = new Color(37, 99, 235);
        Color successColor = new Color(34, 197, 94);
        
        paymentButton.setBackground(successColor);
        paymentButton.setForeground(Color.WHITE);
        paymentButton.setFocusPainted(false);
        
        receiptButton.setBackground(primaryColor);
        receiptButton.setForeground(Color.WHITE);
        receiptButton.setFocusPainted(false);
        
        refreshButton.setBackground(new Color(107, 114, 128));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        
        // Initially disable buttons
        paymentButton.setEnabled(false);
        receiptButton.setEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Fee Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);
        
        // Button and summary panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.add(paymentButton);
        buttonPanel.add(receiptButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(refreshButton);
        
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        summaryPanel.add(totalOutstandingLabel);
        
        controlPanel.add(buttonPanel, BorderLayout.WEST);
        controlPanel.add(summaryPanel, BorderLayout.EAST);
        
        // Table panel
        JScrollPane scrollPane = new JScrollPane(feesTable);
        scrollPane.setPreferredSize(new Dimension(0, 400));
        
        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        // Table selection
        feesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = feesTable.getSelectedRow() != -1;
                paymentButton.setEnabled(hasSelection);
                receiptButton.setEnabled(hasSelection);
            }
        });
        
        // Search functionality
        searchField.addActionListener(e -> filterTable());
        
        // Button actions
        paymentButton.addActionListener(e -> recordPayment());
        receiptButton.addActionListener(e -> generateReceipt());
        refreshButton.addActionListener(e -> loadFeesData());
    }
    
    private void loadFeesData() {
        tableModel.setRowCount(0);
        Collection<Student> students = DatabaseUtil.getAllStudents();
        double totalOutstanding = 0;
        
        for (Student student : students) {
            double outstanding = student.getOutstandingFees();
            totalOutstanding += outstanding;
            
            String status = outstanding > 0 ? "Pending" : "Paid";
            
            Object[] row = {
                student.getStudentId(),
                student.getFullName(),
                student.getCourse(),
                "₹" + String.format("%.0f", student.getTotalFees()),
                "₹" + String.format("%.0f", student.getFeesPaid()),
                "₹" + String.format("%.0f", outstanding),
                status
            };
            tableModel.addRow(row);
        }
        
        totalOutstandingLabel.setText("Total Outstanding: ₹" + String.format("%.0f", totalOutstanding));
    }
    
    private void filterTable() {
        String searchText = searchField.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = 
            (TableRowSorter<DefaultTableModel>) feesTable.getRowSorter();
        
        if (searchText.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
        }
    }
    
    private void recordPayment() {
        int selectedRow = feesTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        selectedRow = feesTable.convertRowIndexToModel(selectedRow);
        String studentId = (String) tableModel.getValueAt(selectedRow, 0);
        Student student = DatabaseUtil.getStudent(studentId);
        
        if (student != null) {
            String amountStr = JOptionPane.showInputDialog(
                this,
                "Enter payment amount for " + student.getFullName() + 
                "\nOutstanding: ₹" + String.format("%.0f", student.getOutstandingFees()),
                "Record Payment",
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (amountStr != null && !amountStr.trim().isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0 && amount <= student.getOutstandingFees()) {
                        student.setFeesPaid(student.getFeesPaid() + amount);
                        DatabaseUtil.updateStudent(student);
                        loadFeesData();
                        JOptionPane.showMessageDialog(this, 
                            "Payment of ₹" + String.format("%.0f", amount) + " recorded successfully!");
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "Invalid amount. Please enter a valid amount.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid amount format.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void generateReceipt() {
        JOptionPane.showMessageDialog(this, "Receipt generation functionality would be implemented here");
    }
}
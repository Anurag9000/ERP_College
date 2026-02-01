package main.java.gui.panels;

import main.java.data.dao.AssignmentDao;
import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.Assignment;
import main.java.models.AssignmentSubmission;
import main.java.models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Student view for assignments and tests
 */
public class AssignmentsPanel extends JPanel {

    private final User currentUser;
    private final AssignmentDao assignmentDao;
    private DefaultTableModel upcomingModel;
    private DefaultTableModel submittedModel;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public AssignmentsPanel(User currentUser) {
        this.currentUser = currentUser;
        this.assignmentDao = new AssignmentDao();

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Assignments & Tests");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(PastelTheme.BODY_FONT);

        tabs.addTab("Upcoming", createUpcomingPanel());
        tabs.addTab("Submitted", createSubmittedPanel());

        add(tabs, BorderLayout.CENTER);

        loadData();
    }

    private JPanel createUpcomingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PastelTheme.PASTEL_BG);

        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Title", "Type", "Due Date", "Max Marks", "Status", "Action" };
        upcomingModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column
            }
        };

        JTable table = new JTable(upcomingModel);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(35);

        // Add submit button column
        table.getColumn("Action").setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            JButton btn = new JButton("Submit");
            PastelTheme.styleButtonPrimary(btn);
            return btn;
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (col == 5 && row >= 0) { // Action column
                    submitAssignment(row);
                }
            }
        });

        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        PastelTheme.styleButtonSecondary(refreshBtn);
        refreshBtn.addActionListener(e -> loadData());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSubmittedPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PastelTheme.PASTEL_BG);

        JCard card = new JCard(new BorderLayout());

        String[] columns = { "Title", "Submitted At", "Marks", "Feedback", "Status" };
        submittedModel = new DefaultTableModel(columns, 0);

        JTable table = new JTable(submittedModel);
        table.setFont(PastelTheme.BODY_FONT);
        table.setRowHeight(30);

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadData() {
        // Load upcoming assignments
        upcomingModel.setRowCount(0);
        List<Assignment> upcoming = assignmentDao.getUpcomingAssignments(currentUser.getUsername());

        for (Assignment a : upcoming) {
            AssignmentSubmission sub = assignmentDao.getSubmission(a.getAssignmentId(), currentUser.getUsername());
            String status = sub != null ? sub.getStatus().name() : "NOT SUBMITTED";

            upcomingModel.addRow(new Object[] {
                    a.getTitle(),
                    a.getAssignmentType(),
                    a.getDueDate().format(DATE_FORMAT),
                    a.getMaxMarks(),
                    status,
                    "Submit"
            });
        }

        // Load submitted assignments
        submittedModel.setRowCount(0);
        List<AssignmentSubmission> submitted = assignmentDao.getSubmissionsByStudent(currentUser.getUsername());
        for (AssignmentSubmission s : submitted) {
            submittedModel.addRow(new Object[] {
                    s.getAssignmentTitle(),
                    s.getSubmittedAt().format(DATE_FORMAT),
                    s.getMarksObtained() != null ? s.getMarksObtained() : "NOT GRADED",
                    s.getFeedback() != null ? s.getFeedback() : "N/A",
                    s.getStatus().name()
            });
        }
    }

    private void submitAssignment(int row) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Get assignment from row
            List<Assignment> upcoming = assignmentDao.getUpcomingAssignments(currentUser.getUsername());
            if (row < upcoming.size()) {
                Assignment assignment = upcoming.get(row);

                AssignmentSubmission submission = new AssignmentSubmission();
                submission.setAssignmentId(assignment.getAssignmentId());
                submission.setStudentCode(currentUser.getUsername());
                submission.setFilePath(file.getAbsolutePath());
                submission.setStatus(Assignment.SubmissionStatus.SUBMITTED);

                try {
                    assignmentDao.submitAssignment(submission);
                    JOptionPane.showMessageDialog(this, "Assignment submitted successfully!");
                    loadData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error submitting: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}

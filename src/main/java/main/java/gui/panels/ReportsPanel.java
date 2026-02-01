package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.*;
import main.java.service.GradebookService;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced Reporting Dashboard for Admins
 */
public class ReportsPanel extends JPanel {

        private JTabbedPane reportTabs;

        public ReportsPanel() {
                setLayout(new BorderLayout(20, 20));
                setBackground(PastelTheme.PASTEL_BG);
                setBorder(new EmptyBorder(20, 20, 20, 20));

                // Header
                JLabel header = new JLabel("Analytics & Reports");
                header.setFont(PastelTheme.HEADER_FONT);
                header.setForeground(PastelTheme.TEXT_PRIMARY);
                add(header, BorderLayout.NORTH);

                // Report Tabs
                reportTabs = new JTabbedPane();
                reportTabs.setFont(PastelTheme.BODY_FONT);

                reportTabs.addTab("Enrollment Trends", createEnrollmentTrendsPanel());
                reportTabs.addTab("Waitlist Pressure", createWaitlistPressurePanel());
                reportTabs.addTab("Attendance Compliance", createAttendanceCompliancePanel());
                reportTabs.addTab("Financial Arrears", createFinancialArrearsPanel());
                reportTabs.addTab("Grade Distribution", createGradeDistributionPanel());

                add(reportTabs, BorderLayout.CENTER);
        }

        private JPanel createEnrollmentTrendsPanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);

                JCard card = new JCard(new BorderLayout());

                // Summary stats
                JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
                statsPanel.setOpaque(false);

                JLabel lblSections = new JLabel("-");
                JLabel lblEnrolled = new JLabel("-");
                JLabel lblCapacity = new JLabel("-");
                JLabel lblUtil = new JLabel("-");

                statsPanel.add(createStatCard("Total Sections", lblSections, PastelTheme.PASTEL_BLUE_DARK));
                statsPanel.add(createStatCard("Total Enrolled", lblEnrolled, PastelTheme.PASTEL_GREEN_DARK));
                statsPanel.add(createStatCard("Total Capacity", lblCapacity, PastelTheme.PASTEL_PURPLE_DARK));
                statsPanel.add(createStatCard("Avg Utilization", lblUtil, PastelTheme.PASTEL_YELLOW_DARK));

                card.add(statsPanel, BorderLayout.NORTH);

                // Detailed table
                String[] columns = { "Course", "Section", "Enrolled", "Capacity", "Utilization %", "Waitlist" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                table.setFont(PastelTheme.BODY_FONT);
                card.add(new JScrollPane(table), BorderLayout.CENTER);

                // Export button
                JButton exportBtn = new JButton("Export to CSV");
                PastelTheme.styleButtonPrimary(exportBtn);
                exportBtn.addActionListener(e -> exportEnrollmentTrends());

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                btnPanel.setOpaque(false);
                btnPanel.add(exportBtn);
                card.add(btnPanel, BorderLayout.SOUTH);
                panel.add(card, BorderLayout.CENTER);

                new SwingWorker<EnrollmentStats, Void>() {
                        @Override
                        protected EnrollmentStats doInBackground() {
                                List<Section> sections = DatabaseUtil.getAllSections();
                                int totalEnrolled = sections.stream().mapToInt(s -> s.getEnrolledStudentIds().size())
                                                .sum();
                                int totalCapacity = sections.stream().mapToInt(Section::getCapacity).sum();
                                double avgUtil = totalCapacity > 0 ? (totalEnrolled * 100.0 / totalCapacity) : 0;

                                List<Object[]> rows = new ArrayList<>();
                                for (Section s : sections) {
                                        int enrolled = s.getEnrolledStudentIds().size();
                                        int cap = s.getCapacity();
                                        double u = cap > 0 ? (enrolled * 100.0 / cap) : 0;
                                        rows.add(new Object[] { s.getCourseId(), s.getSectionId(), enrolled, cap,
                                                        String.format("%.1f%%", u),
                                                        s.getWaitlistedStudentIds().size() });
                                }
                                return new EnrollmentStats(sections.size(), totalEnrolled, totalCapacity, avgUtil,
                                                rows);
                        }

                        @Override
                        protected void done() {
                                try {
                                        EnrollmentStats s = get();
                                        lblSections.setText(String.valueOf(s.sections));
                                        lblEnrolled.setText(String.valueOf(s.enrolled));
                                        lblCapacity.setText(String.valueOf(s.capacity));
                                        lblUtil.setText(String.format("%.1f%%", s.utilization));
                                        model.setRowCount(0);
                                        for (Object[] row : s.rows)
                                                model.addRow(row);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                                        "Error loading enrollment data: " + e.getMessage(), "Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }.execute();

                return panel;
        }

        private static record EnrollmentStats(int sections, int enrolled, int capacity, double utilization,
                        List<Object[]> rows) {
        }

        private JPanel createWaitlistPressurePanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);
                JCard card = new JCard(new BorderLayout());

                JLabel title = new JLabel("Sections with High Waitlist Pressure");
                title.setFont(PastelTheme.CARD_TITLE_FONT);
                title.setBorder(new EmptyBorder(0, 0, 10, 0));
                card.add(title, BorderLayout.NORTH);

                String[] columns = { "Section", "Course", "Enrolled", "Capacity", "Waitlist", "Pressure" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);

                new SwingWorker<List<Object[]>, Void>() {
                        @Override
                        protected List<Object[]> doInBackground() {
                                return DatabaseUtil.getAllSections().stream()
                                                .filter(s -> s.getWaitlistedStudentIds().size() > 0)
                                                .sorted((a, b) -> Integer.compare(b.getWaitlistedStudentIds().size(),
                                                                a.getWaitlistedStudentIds().size()))
                                                .map(s -> {
                                                        int wl = s.getWaitlistedStudentIds().size();
                                                        int cap = s.getCapacity();
                                                        String p = wl > cap * 0.5 ? "HIGH"
                                                                        : wl > cap * 0.2 ? "MEDIUM" : "LOW";
                                                        return new Object[] { s.getSectionId(), s.getCourseId(),
                                                                        s.getEnrolledStudentIds().size(), cap, wl, p };
                                                }).collect(Collectors.toList());
                        }

                        @Override
                        protected void done() {
                                try {
                                        List<Object[]> rows = get();
                                        model.setRowCount(0);
                                        for (Object[] row : rows)
                                                model.addRow(row);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                                        "Error loading waitlist data: " + e.getMessage(), "Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }.execute();

                return panel;
        }

        private JPanel createAttendanceCompliancePanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);
                JCard card = new JCard(new BorderLayout());

                JLabel title = new JLabel("Students Below Attendance Threshold (75%)");
                title.setFont(PastelTheme.CARD_TITLE_FONT);
                title.setBorder(new EmptyBorder(0, 0, 10, 0));
                card.add(title, BorderLayout.NORTH);

                String[] columns = { "Student ID", "Name", "Course", "Attendance %", "Status" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);

                new SwingWorker<List<Object[]>, Void>() {
                        @Override
                        protected List<Object[]> doInBackground() {
                                List<Object[]> rows = new ArrayList<>();
                                for (Section section : DatabaseUtil.getAllSections()) {
                                        List<AttendanceRecord> history = DatabaseUtil
                                                        .getAttendanceForSection(section.getSectionId());
                                        if (history.isEmpty())
                                                continue;

                                        Map<String, Integer> sessions = new HashMap<>();
                                        Map<String, Integer> present = new HashMap<>();
                                        for (AttendanceRecord r : history) {
                                                r.getStatusByStudent().forEach((sid, status) -> {
                                                        sessions.merge(sid, 1, Integer::sum);
                                                        if (status == AttendanceRecord.AttendanceStatus.PRESENT
                                                                        || status == AttendanceRecord.AttendanceStatus.LATE)
                                                                present.merge(sid, 1, Integer::sum);
                                                });
                                        }

                                        for (String sid : section.getEnrolledStudentIds()) {
                                                int tot = sessions.getOrDefault(sid, 0);
                                                if (tot == 0)
                                                        continue;
                                                double pct = (present.getOrDefault(sid, 0) * 100.0) / tot;
                                                if (pct < 75.0) {
                                                        Student s = DatabaseUtil.getStudent(sid);
                                                        rows.add(new Object[] { sid,
                                                                        s != null ? s.getFullName() : "Unknown",
                                                                        section.getCourseId(),
                                                                        String.format("%.1f%%", pct), "AT RISK" });
                                                }
                                        }
                                }
                                return rows;
                        }

                        @Override
                        protected void done() {
                                try {
                                        List<Object[]> rows = get();
                                        model.setRowCount(0);
                                        for (Object[] row : rows)
                                                model.addRow(row);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                                        "Error loading attendance data: " + e.getMessage(), "Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }.execute();

                return panel;
        }

        private JPanel createFinancialArrearsPanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);
                JCard card = new JCard(new BorderLayout());

                JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 0));
                summaryPanel.setOpaque(false);
                JLabel lblOut = new JLabel("-");
                JLabel lblCount = new JLabel("-");
                JLabel lblRate = new JLabel("-");
                summaryPanel.add(createStatCard("Total Outstanding", lblOut, PastelTheme.PASTEL_RED_DARK));
                summaryPanel.add(createStatCard("Students w/ Arrears", lblCount, PastelTheme.PASTEL_YELLOW_DARK));
                summaryPanel.add(createStatCard("Collection Rate", lblRate, PastelTheme.PASTEL_GREEN_DARK));
                card.add(summaryPanel, BorderLayout.NORTH);

                String[] columns = { "Student ID", "Name", "Total Fees", "Paid", "Outstanding", "Due Date" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);

                new SwingWorker<FinanceStats, Void>() {
                        @Override
                        protected FinanceStats doInBackground() {
                                List<Student> students = DatabaseUtil.getAllStudents();
                                double total = students.stream().mapToDouble(Student::getOutstandingFees).sum();
                                long count = students.stream().filter(s -> s.getOutstandingFees() > 0).count();
                                double rate = students.stream()
                                                .mapToDouble(s -> s.getTotalFees() > 0
                                                                ? (s.getFeesPaid() / s.getTotalFees() * 100)
                                                                : 0)
                                                .average().orElse(0);
                                List<Object[]> rows = students.stream()
                                                .filter(s -> s.getOutstandingFees() > 0)
                                                .sorted((a, b) -> Double.compare(b.getOutstandingFees(),
                                                                a.getOutstandingFees()))
                                                .map(s -> new Object[] {
                                                                s.getStudentId(), s.getFullName(),
                                                                String.format("₹%.2f", s.getTotalFees()),
                                                                String.format("₹%.2f", s.getFeesPaid()),
                                                                String.format("₹%.2f", s.getOutstandingFees()),
                                                                s.getNextFeeDueDate() != null
                                                                                ? s.getNextFeeDueDate().toString()
                                                                                : "N/A"
                                                }).collect(Collectors.toList());
                                return new FinanceStats(total, count, rate, rows);
                        }

                        @Override
                        protected void done() {
                                try {
                                        FinanceStats s = get();
                                        lblOut.setText(String.format("₹%.2f", s.total));
                                        lblCount.setText(String.valueOf(s.count));
                                        lblRate.setText(String.format("%.1f%%", s.rate));
                                        model.setRowCount(0);
                                        for (Object[] row : s.rows)
                                                model.addRow(row);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                                        "Error loading financial data: " + e.getMessage(), "Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }.execute();

                return panel;
        }

        private static record FinanceStats(double total, long count, double rate, List<Object[]> rows) {
        }

        private JPanel createGradeDistributionPanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);
                JCard card = new JCard(new BorderLayout());

                JLabel title = new JLabel("Grade Distribution by Course");
                title.setFont(PastelTheme.CARD_TITLE_FONT);
                title.setBorder(new EmptyBorder(0, 0, 10, 0));
                card.add(title, BorderLayout.NORTH);

                String[] columns = { "Course", "Section", "A+", "A", "B+", "B", "C", "F", "Avg" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);

                new SwingWorker<List<Object[]>, Void>() {
                        @Override
                        protected List<Object[]> doInBackground() {
                                List<Object[]> rows = new ArrayList<>();
                                for (Section s : DatabaseUtil.getAllSections()) {
                                        List<EnrollmentRecord> enrollments = DatabaseUtil.getEnrollmentDao()
                                                        .findBySection(s.getSectionId());
                                        if (enrollments.isEmpty())
                                                continue;
                                        int ap = 0, a = 0, bp = 0, b = 0, c = 0, f = 0;
                                        double total = 0;
                                        int count = 0;
                                        for (EnrollmentRecord r : enrollments) {
                                                if (r.getStatus() != EnrollmentRecord.Status.ENROLLED)
                                                        continue;
                                                double score = r.getFinalGrade();
                                                if (score <= 0 && !r.getComponentScores().isEmpty())
                                                        score = s.computeFinalScore(r.getComponentScores());
                                                total += score;
                                                count++;
                                                double pts = GradebookService.calculateRelativePoints(score,
                                                                s.getSectionId());
                                                if (pts >= 9.0)
                                                        ap++;
                                                else if (pts >= 8.0)
                                                        a++;
                                                else if (pts >= 7.0)
                                                        bp++;
                                                else if (pts >= 6.0)
                                                        b++;
                                                else if (pts >= 5.0)
                                                        c++;
                                                else
                                                        f++;
                                        }
                                        if (count > 0) {
                                                rows.add(new Object[] { s.getCourseId(), s.getSectionId(), ap, a, bp, b,
                                                                c, f, String.format("%.1f", total / count) });
                                        }
                                }
                                return rows;
                        }

                        @Override
                        protected void done() {
                                try {
                                        List<Object[]> rows = get();
                                        model.setRowCount(0);
                                        for (Object[] row : rows)
                                                model.addRow(row);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(ReportsPanel.this,
                                                        "Error loading grade data: " + e.getMessage(), "Error",
                                                        JOptionPane.ERROR_MESSAGE);
                                }
                        }
                }.execute();

                return panel;
        }

        private JPanel createStatCard(String label, JLabel lblValue, Color color) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

                lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
                lblValue.setForeground(color);

                JLabel lblLabel = new JLabel(label);
                lblLabel.setFont(PastelTheme.BODY_FONT);
                lblLabel.setForeground(PastelTheme.TEXT_SECONDARY);

                card.add(lblValue, BorderLayout.CENTER);
                card.add(lblLabel, BorderLayout.SOUTH);
                return card;
        }

        private void exportEnrollmentTrends() {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new java.io.File("enrollment_report.csv"));
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        new SwingWorker<Void, Void>() {
                                @Override
                                protected Void doInBackground() throws Exception {
                                        java.io.File file = chooser.getSelectedFile();
                                        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                                                writer.write("Course,Section,Enrolled,Capacity,Utilization,Waitlist\n");
                                                for (Section s : DatabaseUtil.getAllSections()) {
                                                        int enrolled = s.getEnrolledStudentIds().size();
                                                        int capacity = s.getCapacity();
                                                        double util = capacity > 0 ? (enrolled * 100.0 / capacity) : 0;
                                                        writer.write(String.format("%s,%s,%d,%d,%.1f%%,%d\n",
                                                                        s.getCourseId(), s.getSectionId(), enrolled,
                                                                        capacity, util,
                                                                        s.getWaitlistedStudentIds().size()));
                                                }
                                        }
                                        return null;
                                }

                                @Override
                                protected void done() {
                                        try {
                                                get();
                                                JOptionPane.showMessageDialog(ReportsPanel.this,
                                                                "Enrollment trends exported.");
                                        } catch (Exception ex) {
                                                JOptionPane.showMessageDialog(ReportsPanel.this,
                                                                "Error exporting report: " + ex.getMessage(), "Error",
                                                                JOptionPane.ERROR_MESSAGE);
                                        }
                                }
                        }.execute();
                }
        }
}

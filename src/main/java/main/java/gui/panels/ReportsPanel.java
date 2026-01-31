package main.java.gui.panels;

import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.*;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
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

                List<Section> allSections = DatabaseUtil.getAllSections();
                int totalSections = allSections.size();
                int totalEnrolled = allSections.stream()
                                .mapToInt(s -> s.getEnrolledStudentIds().size())
                                .sum();
                int totalCapacity = allSections.stream()
                                .mapToInt(Section::getCapacity)
                                .sum();
                double avgUtilization = totalCapacity > 0 ? (totalEnrolled * 100.0 / totalCapacity) : 0;

                statsPanel.add(createStatCard("Total Sections", String.valueOf(totalSections),
                                PastelTheme.PASTEL_BLUE_DARK));
                statsPanel.add(createStatCard("Total Enrolled", String.valueOf(totalEnrolled),
                                PastelTheme.PASTEL_GREEN_DARK));
                statsPanel.add(createStatCard("Total Capacity", String.valueOf(totalCapacity),
                                PastelTheme.PASTEL_PURPLE_DARK));
                statsPanel.add(createStatCard("Avg Utilization", String.format("%.1f%%", avgUtilization),
                                PastelTheme.PASTEL_YELLOW_DARK));

                card.add(statsPanel, BorderLayout.NORTH);

                // Detailed table
                String[] columns = { "Course", "Section", "Enrolled", "Capacity", "Utilization %", "Waitlist" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);
                table.setFont(PastelTheme.BODY_FONT);

                for (Section s : allSections) {
                        int enrolled = s.getEnrolledStudentIds().size();
                        int capacity = s.getCapacity();
                        double util = capacity > 0 ? (enrolled * 100.0 / capacity) : 0;
                        int waitlist = s.getWaitlistedStudentIds().size();

                        model.addRow(new Object[] {
                                        s.getCourseId(),
                                        s.getSectionId(),
                                        enrolled,
                                        capacity,
                                        String.format("%.1f%%", util),
                                        waitlist
                        });
                }

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
                return panel;
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

                List<Section> sections = DatabaseUtil.getAllSections().stream()
                                .filter(s -> s.getWaitlistedStudentIds().size() > 0)
                                .sorted((a, b) -> Integer.compare(b.getWaitlistedStudentIds().size(),
                                                a.getWaitlistedStudentIds().size()))
                                .collect(Collectors.toList());

                for (Section s : sections) {
                        int waitlist = s.getWaitlistedStudentIds().size();
                        int capacity = s.getCapacity();
                        String pressure = waitlist > capacity * 0.5 ? "HIGH"
                                        : waitlist > capacity * 0.2 ? "MEDIUM" : "LOW";

                        model.addRow(new Object[] {
                                        s.getSectionId(),
                                        s.getCourseId(),
                                        s.getEnrolledStudentIds().size(),
                                        capacity,
                                        waitlist,
                                        pressure
                        });
                }

                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);
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

                // Mock data - in real implementation, calculate from attendance records
                List<Student> students = DatabaseUtil.getAllStudents();
                for (Student student : students.subList(0, Math.min(10, students.size()))) {
                        double mockAttendance = 60 + Math.random() * 30; // Mock 60-90%
                        String status = mockAttendance < 75 ? "AT RISK" : "OK";

                        model.addRow(new Object[] {
                                        student.getStudentId(),
                                        student.getFullName(),
                                        student.getCourse(),
                                        String.format("%.1f%%", mockAttendance),
                                        status
                        });
                }

                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);
                return panel;
        }

        private JPanel createFinancialArrearsPanel() {
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(PastelTheme.PASTEL_BG);

                JCard card = new JCard(new BorderLayout());

                // Summary
                JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 0));
                summaryPanel.setOpaque(false);

                List<Student> students = DatabaseUtil.getAllStudents();
                double totalOutstanding = students.stream()
                                .mapToDouble(Student::getOutstandingFees)
                                .sum();
                long studentsWithArrears = students.stream()
                                .filter(s -> s.getOutstandingFees() > 0)
                                .count();

                summaryPanel.add(createStatCard("Total Outstanding", String.format("₹%.2f", totalOutstanding),
                                PastelTheme.PASTEL_RED_DARK));
                summaryPanel.add(createStatCard("Students w/ Arrears", String.valueOf(studentsWithArrears),
                                PastelTheme.PASTEL_YELLOW_DARK));
                summaryPanel.add(createStatCard("Collection Rate", String.format("%.1f%%",
                                students.stream()
                                                .mapToDouble(s -> s.getTotalFees() > 0
                                                                ? (s.getFeesPaid() / s.getTotalFees() * 100)
                                                                : 0)
                                                .average().orElse(0)),
                                PastelTheme.PASTEL_GREEN_DARK));

                card.add(summaryPanel, BorderLayout.NORTH);

                // Detailed table
                String[] columns = { "Student ID", "Name", "Total Fees", "Paid", "Outstanding", "Due Date" };
                DefaultTableModel model = new DefaultTableModel(columns, 0);
                JTable table = new JTable(model);

                students.stream()
                                .filter(s -> s.getOutstandingFees() > 0)
                                .sorted((a, b) -> Double.compare(b.getOutstandingFees(), a.getOutstandingFees()))
                                .forEach(s -> {
                                        model.addRow(new Object[] {
                                                        s.getStudentId(),
                                                        s.getFullName(),
                                                        String.format("₹%.2f", s.getTotalFees()),
                                                        String.format("₹%.2f", s.getFeesPaid()),
                                                        String.format("₹%.2f", s.getOutstandingFees()),
                                                        s.getNextFeeDueDate() != null ? s.getNextFeeDueDate().toString()
                                                                        : "N/A"
                                        });
                                });

                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);
                return panel;
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

                // Mock data - in real implementation, calculate from grades
                List<Section> sections = DatabaseUtil.getAllSections();
                for (Section s : sections.subList(0, Math.min(15, sections.size()))) {
                        int enrolled = s.getEnrolledStudentIds().size();
                        if (enrolled > 0) {
                                // Mock distribution
                                model.addRow(new Object[] {
                                                s.getCourseId(),
                                                s.getSectionId(),
                                                (int) (enrolled * 0.1),
                                                (int) (enrolled * 0.2),
                                                (int) (enrolled * 0.3),
                                                (int) (enrolled * 0.2),
                                                (int) (enrolled * 0.15),
                                                (int) (enrolled * 0.05),
                                                String.format("%.1f", 70 + Math.random() * 20)
                                });
                        }
                }

                card.add(new JScrollPane(table), BorderLayout.CENTER);
                panel.add(card, BorderLayout.CENTER);
                return panel;
        }

        private JPanel createStatCard(String label, String value, Color color) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

                JLabel lblValue = new JLabel(value);
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
                        java.io.File file = chooser.getSelectedFile();
                        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                                writer.write("Course,Section,Enrolled,Capacity,Utilization,Waitlist\n");
                                for (Section s : DatabaseUtil.getAllSections()) {
                                        int enrolled = s.getEnrolledStudentIds().size();
                                        int capacity = s.getCapacity();
                                        double util = capacity > 0 ? (enrolled * 100.0 / capacity) : 0;
                                        writer.write(String.format("%s,%s,%d,%d,%.1f%%,%d\n",
                                                        s.getCourseId(), s.getSectionId(), enrolled, capacity, util,
                                                        s.getWaitlistedStudentIds().size()));
                                }
                                JOptionPane.showMessageDialog(this,
                                                "Enrollment trends exported to " + file.getAbsolutePath());
                        } catch (java.io.IOException ex) {
                                JOptionPane.showMessageDialog(this, "Error exporting report: " + ex.getMessage(),
                                                "Export Error",
                                                JOptionPane.ERROR_MESSAGE);
                        }
                }
        }
}

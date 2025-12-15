package main.java.gui.panels;

import main.java.data.dao.ExamDao;
import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.ExamForm;
import main.java.models.Section;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Examination module panel for students
 */
public class ExaminationPanel extends JPanel {

    private final User currentUser;
    private final ExamDao examDao;
    private JLabel statusLabel;
    private JButton submitFormBtn;
    private JButton downloadAdmitCardBtn;

    public ExaminationPanel(User currentUser) {
        this.currentUser = currentUser;
        this.examDao = new ExamDao();

        setLayout(new BorderLayout(20, 20));
        setBackground(PastelTheme.PASTEL_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel header = new JLabel("Examination Portal");
        header.setFont(PastelTheme.HEADER_FONT);
        header.setForeground(PastelTheme.TEXT_PRIMARY);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(PastelTheme.PASTEL_BG);

        mainPanel.add(createFormCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createAdmitCardCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createDateSheetCard());

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        checkExamFormStatus();
    }

    private JPanel createFormCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Exam Form Submission");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        contentPanel.setOpaque(false);

        statusLabel = new JLabel("Status: Not Submitted");
        statusLabel.setFont(PastelTheme.BODY_FONT);
        contentPanel.add(statusLabel);

        JLabel infoLabel = new JLabel("Submit your exam form for the current semester");
        infoLabel.setFont(PastelTheme.BODY_FONT);
        infoLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        contentPanel.add(infoLabel);

        submitFormBtn = new JButton("Submit Exam Form");
        PastelTheme.styleButtonPrimary(submitFormBtn);
        submitFormBtn.addActionListener(e -> submitExamForm());
        contentPanel.add(submitFormBtn);

        card.add(contentPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAdmitCardCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Admit Card");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        contentPanel.setOpaque(false);

        JLabel infoLabel = new JLabel("Download your admit card after form approval");
        infoLabel.setFont(PastelTheme.BODY_FONT);
        infoLabel.setForeground(PastelTheme.TEXT_SECONDARY);
        contentPanel.add(infoLabel);

        downloadAdmitCardBtn = new JButton("Download Admit Card");
        PastelTheme.styleButtonPrimary(downloadAdmitCardBtn);
        downloadAdmitCardBtn.setEnabled(false);
        downloadAdmitCardBtn.addActionListener(e -> downloadAdmitCard());
        contentPanel.add(downloadAdmitCardBtn);

        card.add(contentPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDateSheetCard() {
        JCard card = new JCard(new BorderLayout(10, 10));

        JLabel title = new JLabel("Exam Date Sheet");
        title.setFont(PastelTheme.CARD_TITLE_FONT);
        card.add(title, BorderLayout.NORTH);

        String[] columns = { "Course", "Date", "Time", "Room" };
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Exam schedule will be published soon");

        JList<String> list = new JList<>(model);
        list.setFont(PastelTheme.BODY_FONT);

        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }

    private void submitExamForm() {
        // Get current semester sections
        List<Section> enrolledSections = new ArrayList<>();
        // TODO: Get from DatabaseUtil

        if (enrolledSections.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No enrolled sections found for current semester",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Submit exam form for " + enrolledSections.size() + " courses?",
                "Confirm Submission",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ExamForm form = new ExamForm();
            form.setStudentCode(currentUser.getUsername());
            form.setSemester("Fall");
            form.setYear(LocalDate.now().getYear());
            form.setStatus(ExamForm.FormStatus.SUBMITTED);
            form.setExamFeePaid(false);

            List<String> sectionCodes = new ArrayList<>();
            for (Section s : enrolledSections) {
                sectionCodes.add(s.getSectionId());
            }
            form.setSectionCodes(sectionCodes);

            try {
                examDao.submitExamForm(form);
                JOptionPane.showMessageDialog(this,
                        "Exam form submitted successfully!\nPlease pay exam fee to complete registration.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                checkExamFormStatus();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error submitting form: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void checkExamFormStatus() {
        ExamForm form = examDao.getExamForm(currentUser.getUsername(), "Fall", LocalDate.now().getYear());
        if (form != null) {
            statusLabel.setText("Status: " + form.getStatus() +
                    (form.isExamFeePaid() ? " (Fee Paid)" : " (Fee Pending)"));
            submitFormBtn.setEnabled(false);

            if (form.getStatus() == ExamForm.FormStatus.APPROVED && form.isExamFeePaid()) {
                downloadAdmitCardBtn.setEnabled(true);
            }
        }
    }

    private void downloadAdmitCard() {
        JOptionPane.showMessageDialog(this,
                "Admit card downloaded to: admit_card.pdf",
                "Download Complete", JOptionPane.INFORMATION_MESSAGE);
        // TODO: Implement PDF generation
    }
}

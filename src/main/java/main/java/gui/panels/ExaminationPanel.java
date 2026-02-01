package main.java.gui.panels;

import main.java.data.dao.ExamDao;
import main.java.gui.components.JCard;
import main.java.gui.style.PastelTheme;
import main.java.models.ExamForm;
import main.java.models.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;

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

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Exam schedule will be published soon");

        JList<String> list = new JList<>(model);
        list.setFont(PastelTheme.BODY_FONT);

        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }

    private void submitExamForm() {
        // Get current semester sections - typically from students current enrollment
        // For now, we interact with DatabaseUtil to get enrolled sections if possible
        // or allow submission for all active sections if applicable.

        int confirm = JOptionPane.showConfirmDialog(this,
                "Submit exam form for the current semester?",
                "Confirm Submission",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ExamForm form = new ExamForm();
            form.setStudentCode(currentUser.getUsername());
            form.setSemester("Fall");
            form.setYear(LocalDate.now().getYear());
            form.setStatus(ExamForm.FormStatus.SUBMITTED);
            form.setExamFeePaid(false);

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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("admit_card_" + currentUser.getUsername() + ".pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText("COLLEGE ERP - ADMIT CARD");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(100, 650);
                    contentStream.showText("Student Name: " + currentUser.getFullName());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Username: " + currentUser.getUsername());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Date: " + LocalDate.now());
                    contentStream.newLineAtOffset(0, -40);
                    contentStream.showText("EXAMINATION DETAILS:");
                    contentStream.newLineAtOffset(0, -20);

                    ExamForm form = examDao.getExamForm(currentUser.getUsername(), "Fall", LocalDate.now().getYear());
                    if (form != null && form.getSectionCodes() != null) {
                        for (String sectionCode : form.getSectionCodes()) {
                            contentStream.showText("- " + sectionCode);
                            contentStream.newLineAtOffset(0, -15);
                        }
                    }
                    contentStream.endText();
                }

                document.save(saveFile);
                JOptionPane.showMessageDialog(this,
                        "Admit card saved to: " + saveFile.getAbsolutePath(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error generating PDF: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

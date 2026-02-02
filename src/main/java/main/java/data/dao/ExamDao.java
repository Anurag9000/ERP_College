package main.java.data.dao;

import main.java.models.ExamForm;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDao extends BaseDao {

    public ExamDao() {
        super(main.java.config.DataSourceRegistry.erpDataSource().orElse(null));
    }

    /**
     * Submits an exam form for a student.
     * 
     * @param form the exam form to submit (must not be null with valid data)
     * @throws IllegalArgumentException if form is null or has invalid data
     * @throws RuntimeException         if database operation fails
     */
    public void submitExamForm(ExamForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Exam form cannot be null");
        }
        if (form.getStudentCode() == null || form.getStudentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        if (form.getSemester() == null || form.getSemester().trim().isEmpty()) {
            throw new IllegalArgumentException("Semester cannot be null or empty");
        }
        if (form.getStatus() == null) {
            throw new IllegalArgumentException("Form status cannot be null");
        }
        String sql = "INSERT INTO exam_forms (student_code, semester, year, status, exam_fee_paid) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();

                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, form.getStudentCode());
            stmt.setString(2, form.getSemester());
            stmt.setInt(3, form.getYear());
            stmt.setString(4, form.getStatus().name());
            stmt.setBoolean(5, form.isExamFeePaid());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    form.setFormId(rs.getLong(1));
                    insertExamRegistrations(form.getFormId(), form.getSectionCodes());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error submitting exam form", e);
        }
    }

    private void insertExamRegistrations(long formId, List<String> sectionCodes) {
        String sql = "INSERT INTO exam_registrations (form_id, section_code) VALUES (?, ?)";
        try (Connection conn = getConnection();

                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String sectionCode : sectionCodes) {
                stmt.setLong(1, formId);
                stmt.setString(2, sectionCode);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting exam registrations", e);
        }
    }

    /**
     * Retrieves an exam form for a student.
     * 
     * @param studentCode the student code (must not be null or empty)
     * @param semester    the semester (must not be null or empty)
     * @param year        the year
     * @return the exam form if found, null otherwise
     * @throws IllegalArgumentException if parameters are null or empty
     */
    public ExamForm getExamForm(String studentCode, String semester, int year) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Student code cannot be null or empty");
        }
        if (semester == null || semester.trim().isEmpty()) {
            throw new IllegalArgumentException("Semester cannot be null or empty");
        }
        String sql = "SELECT * FROM exam_forms WHERE student_code = ? AND semester = ? AND year = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentCode);
            stmt.setString(2, semester);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ExamForm form = mapExamForm(rs);
                    form.setSectionCodes(getRegisteredSections(form.getFormId()));
                    return form;
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching exam form for student {} in {} {}: {}", studentCode, semester, year,
                    e.getMessage(), e);
        }
        return null;
    }

    private List<String> getRegisteredSections(long formId) {
        String sql = "SELECT section_code FROM exam_registrations WHERE form_id = ?";
        List<String> sections = new ArrayList<>();
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, formId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sections.add(rs.getString("section_code"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching registered sections for form {}: {}", formId, e.getMessage(), e);
        }
        return sections;
    }

    /**
     * Updates the fee payment status for an exam form.
     * 
     * @param formId the form ID (must be greater than 0)
     * @param paid   the payment status
     * @throws IllegalArgumentException if formId is invalid
     * @throws RuntimeException         if database operation fails
     */
    public void updateFeePaymentStatus(long formId, boolean paid) {
        if (formId <= 0) {
            throw new IllegalArgumentException("Form ID must be greater than 0");
        }
        String sql = "UPDATE exam_forms SET exam_fee_paid = ? WHERE form_id = ?";
        try (Connection conn = getConnection();

                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, paid);
            stmt.setLong(2, formId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating fee payment status", e);
        }
    }

    /**
     * Generates an admit card for an exam form.
     * 
     * @param formId  the form ID (must be greater than 0)
     * @param pdfPath the PDF path (must not be null or empty)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException         if database operation fails
     */
    public void generateAdmitCard(long formId, String pdfPath) {
        if (formId <= 0) {
            throw new IllegalArgumentException("Form ID must be greater than 0");
        }
        if (pdfPath == null || pdfPath.trim().isEmpty()) {
            throw new IllegalArgumentException("PDF path cannot be null or empty");
        }
        String sql = "INSERT INTO admit_cards (form_id, pdf_path) VALUES (?, ?)";
        try (Connection conn = getConnection();

                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, formId);
            stmt.setString(2, pdfPath);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error generating admit card", e);
        }
    }

    private ExamForm mapExamForm(ResultSet rs) throws SQLException {
        ExamForm form = new ExamForm();
        form.setFormId(rs.getLong("form_id"));
        form.setStudentCode(rs.getString("student_code"));
        form.setSemester(rs.getString("semester"));
        form.setYear(rs.getInt("year"));
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            form.setSubmittedAt(submittedAt.toLocalDateTime());
        }
        form.setStatus(ExamForm.FormStatus.valueOf(rs.getString("status")));
        form.setExamFeePaid(rs.getBoolean("exam_fee_paid"));
        return form;
    }
}

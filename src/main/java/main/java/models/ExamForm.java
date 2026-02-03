package main.java.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamForm implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum FormStatus {
        SUBMITTED, APPROVED, REJECTED
    }

    private long formId;
    private String studentCode;
    private String semester;
    private int year;
    private LocalDateTime submittedAt;
    private FormStatus status;
    private boolean examFeePaid;
    private List<String> sectionCodes;

    public ExamForm() {
        this.sectionCodes = new ArrayList<>();
    }

    public long getFormId() {
        return formId;
    }

    public void setFormId(long formId) {
        this.formId = formId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getYear() {
        return year;
    }

    /**
     * Sets the year.
     * 
     * @param year the year (must be between 2000 and 2100)
     * @throws IllegalArgumentException if year is out of range
     */
    public void setYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100, got: " + year);
        }
        this.year = year;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public FormStatus getStatus() {
        return status;
    }

    public void setStatus(FormStatus status) {
        this.status = status;
    }

    public boolean isExamFeePaid() {
        return examFeePaid;
    }

    public void setExamFeePaid(boolean examFeePaid) {
        this.examFeePaid = examFeePaid;
    }

    public List<String> getSectionCodes() {
        return sectionCodes;
    }

    public void setSectionCodes(List<String> sectionCodes) {
        this.sectionCodes = sectionCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ExamForm))
            return false;
        ExamForm examForm = (ExamForm) o;
        return formId == examForm.formId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(formId);
    }
}

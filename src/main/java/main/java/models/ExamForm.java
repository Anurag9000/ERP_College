package main.java.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamForm {
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

    public void setYear(int year) {
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
}

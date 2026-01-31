package main.java.models;

import java.time.LocalDateTime;

public class AssignmentSubmission implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long submissionId;
    private long assignmentId;
    private String studentCode;
    private LocalDateTime submittedAt;
    private String filePath;
    private Double marksObtained;
    private String feedback;
    private Assignment.SubmissionStatus status;

    // Transients
    private String studentName;
    private String assignmentTitle;

    public AssignmentSubmission() {
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Double getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(Double marksObtained) {
        this.marksObtained = marksObtained;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Assignment.SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(Assignment.SubmissionStatus status) {
        this.status = status;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignmentSubmission))
            return false;
        AssignmentSubmission that = (AssignmentSubmission) o;
        return submissionId == that.submissionId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(submissionId);
    }
}

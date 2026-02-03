package main.java.models;

import java.time.LocalDateTime;

public class Assignment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum AssignmentType {
        ASSIGNMENT, TEST, QUIZ, PROJECT
    }

    public enum SubmissionStatus {
        SUBMITTED, GRADED, LATE, PENDING
    }

    private long assignmentId;
    private String sectionCode;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private double maxMarks;
    private AssignmentType assignmentType;
    private LocalDateTime createdAt;

    public Assignment() {
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    /**
     * Sets the maximum marks.
     * 
     * @param maxMarks the maximum marks (must be positive)
     * @throws IllegalArgumentException if maxMarks is not positive
     */
    public void setMaxMarks(double maxMarks) {
        if (maxMarks <= 0) {
            throw new IllegalArgumentException("Max marks must be positive, got: " + maxMarks);
        }
        this.maxMarks = maxMarks;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(AssignmentType assignmentType) {
        this.assignmentType = assignmentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Assignment))
            return false;
        Assignment that = (Assignment) o;
        return assignmentId == that.assignmentId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(assignmentId);
    }
}

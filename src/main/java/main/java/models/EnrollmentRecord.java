package main.java.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a student's enrollment state for a specific section.
 */
public class EnrollmentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        ENROLLED,
        WAITLISTED,
        DROPPED
    }

    private long id;
    private String studentId;
    private String sectionId;
    private Status status;
    private LocalDateTime updatedAt;
    private Map<String, Double> componentScores;
    private Map<String, String> componentFeedback;
    private double finalGrade;
    private Map<String, Double> weighting;

    public EnrollmentRecord() {
        this.componentScores = new LinkedHashMap<>();
        this.componentFeedback = new LinkedHashMap<>();
        this.weighting = new LinkedHashMap<>();
    }

    public EnrollmentRecord(String studentId, String sectionId, Status status) {
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        this.componentScores = new LinkedHashMap<>();
        this.componentFeedback = new LinkedHashMap<>();
        this.weighting = new LinkedHashMap<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Double> getComponentScores() {
        return componentScores;
    }

    public void setComponentScores(Map<String, Double> componentScores) {
        this.componentScores = new LinkedHashMap<>(componentScores);
        this.updatedAt = LocalDateTime.now();
    }

    public void putScore(String component, double value) {
        this.componentScores.put(component, value);
        this.updatedAt = LocalDateTime.now();
    }

    public double getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(double finalGrade) {
        this.finalGrade = finalGrade;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Double> getWeighting() {
        return Collections.unmodifiableMap(weighting);
    }

    public void setWeighting(Map<String, Double> weighting) {
        this.weighting = new LinkedHashMap<>(weighting);
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getComponentFeedback() {
        return componentFeedback == null ? Collections.emptyMap() : Collections.unmodifiableMap(componentFeedback);
    }

    public void setComponentFeedback(Map<String, String> feedback) {
        this.componentFeedback = feedback == null ? new LinkedHashMap<>() : new LinkedHashMap<>(feedback);
        this.updatedAt = LocalDateTime.now();
    }

    public void putFeedback(String component, String comment) {
        if (this.componentFeedback == null) {
            this.componentFeedback = new LinkedHashMap<>();
        }
        this.componentFeedback.put(component, comment);
        this.updatedAt = LocalDateTime.now();
    }
}

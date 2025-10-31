package main.java.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Course model class representing course information
 */
public class Course implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String courseId;
    private String courseName;
    private String department;
    private int duration; // in semesters
    private double fees;
    private String description;
    private List<String> subjects;
    private int totalSeats;
    private int availableSeats;
    private String status;
    private int creditHours;
    
    public Course() {
        this.subjects = new ArrayList<>();
        this.creditHours = 3;
    }
    
    public Course(String courseId, String courseName, String department, 
                 int duration, double fees, String description, int totalSeats) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.department = department;
        this.duration = duration;
        this.fees = fees;
        this.description = description;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.status = "Active";
        this.subjects = new ArrayList<>();
        this.creditHours = 3;
    }
    
    // Getters and Setters
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public double getFees() { return fees; }
    public void setFees(double fees) { this.fees = fees; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
        if (this.availableSeats > totalSeats) {
            this.availableSeats = totalSeats;
        }
    }
    
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getEnrolledStudents() { return totalSeats - availableSeats; }

    public int getCreditHours() { return creditHours; }
    public void setCreditHours(int creditHours) { this.creditHours = creditHours; }
}

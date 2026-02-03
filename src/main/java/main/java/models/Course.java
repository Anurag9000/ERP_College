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
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getDuration() {
        return duration;
    }

    /**
     * Sets the course duration in semesters.
     * 
     * @param duration the duration (must be between 1 and 12)
     * @throws IllegalArgumentException if duration is out of range
     */
    public void setDuration(int duration) {
        if (duration < 1 || duration > 12) {
            throw new IllegalArgumentException("Duration must be between 1 and 12 semesters, got: " + duration);
        }
        this.duration = duration;
    }

    public double getFees() {
        return fees;
    }

    /**
     * Sets the course fees.
     * 
     * @param fees the fees (must not be negative)
     * @throws IllegalArgumentException if fees is negative
     */
    public void setFees(double fees) {
        if (fees < 0) {
            throw new IllegalArgumentException("Fees cannot be negative, got: " + fees);
        }
        this.fees = fees;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    /**
     * Sets the total seats.
     * 
     * @param totalSeats the total seats (must be positive)
     * @throws IllegalArgumentException if totalSeats is not positive
     */
    public void setTotalSeats(int totalSeats) {
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Total seats must be positive, got: " + totalSeats);
        }
        this.totalSeats = totalSeats;
        if (this.availableSeats > totalSeats) {
            this.availableSeats = totalSeats;
        }
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    /**
     * Sets the available seats.
     * 
     * @param availableSeats the available seats (must not be negative)
     * @throws IllegalArgumentException if availableSeats is negative
     */
    public void setAvailableSeats(int availableSeats) {
        if (availableSeats < 0) {
            throw new IllegalArgumentException("Available seats cannot be negative, got: " + availableSeats);
        }
        this.availableSeats = availableSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getEnrolledStudents() {
        return totalSeats - availableSeats;
    }

    public int getCreditHours() {
        return creditHours;
    }

    /**
     * Sets the credit hours.
     * 
     * @param creditHours the credit hours (must be between 1 and 6)
     * @throws IllegalArgumentException if creditHours is out of range
     */
    public void setCreditHours(int creditHours) {
        if (creditHours < 1 || creditHours > 6) {
            throw new IllegalArgumentException("Credit hours must be between 1 and 6, got: " + creditHours);
        }
        this.creditHours = creditHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Course))
            return false;
        Course course = (Course) o;
        return java.util.Objects.equals(courseId, course.courseId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(courseId);
    }
}

package main.java.models;

import java.time.LocalDate;

/**
 * Student model class representing student information
 */
public class Student implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String course;
    private int semester;
    private LocalDate admissionDate;
    private String status;
    private double feesPaid;
    private double totalFees;
    private double cgpa;
    private int creditsCompleted;
    private int creditsInProgress;
    private LocalDate nextFeeDueDate;
    private String advisorId;
    private String academicStanding;
    private String username;
    
    public Student() {}
    
    public Student(String studentId, String firstName, String lastName, 
                  String email, String phone, LocalDate dateOfBirth, 
                  String address, String course, int semester) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.course = course;
        this.semester = semester;
        this.admissionDate = LocalDate.now();
        this.status = "Active";
        this.feesPaid = 0.0;
        this.totalFees = 50000.0; // Default fees
        this.cgpa = 0.0;
        this.creditsCompleted = 0;
        this.creditsInProgress = 0;
        this.nextFeeDueDate = LocalDate.now().plusMonths(1);
        this.academicStanding = "Good";
        this.username = null;
    }
    
    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() { return firstName + " " + lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getFeesPaid() { return feesPaid; }
    public void setFeesPaid(double feesPaid) { this.feesPaid = feesPaid; }
    
    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }
    
    public double getOutstandingFees() { return totalFees - feesPaid; }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getCreditsCompleted() {
        return creditsCompleted;
    }

    public void setCreditsCompleted(int creditsCompleted) {
        this.creditsCompleted = creditsCompleted;
    }

    public int getCreditsInProgress() {
        return creditsInProgress;
    }

    public void setCreditsInProgress(int creditsInProgress) {
        this.creditsInProgress = creditsInProgress;
    }

    public LocalDate getNextFeeDueDate() {
        return nextFeeDueDate;
    }

    public void setNextFeeDueDate(LocalDate nextFeeDueDate) {
        this.nextFeeDueDate = nextFeeDueDate;
    }

    public String getAdvisorId() {
        return advisorId;
    }

    public void setAdvisorId(String advisorId) {
        this.advisorId = advisorId;
    }

    public String getAcademicStanding() {
        return academicStanding;
    }

    public void setAcademicStanding(String academicStanding) {
        this.academicStanding = academicStanding;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getProgressPercent() {
        int total = creditsCompleted + creditsInProgress;
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (creditsCompleted * 100.0) / Math.max(total, 1));
    }
}

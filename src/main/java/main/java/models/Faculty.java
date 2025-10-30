package main.java.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Faculty model class representing faculty information
 */
public class Faculty implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String facultyId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private LocalDate joiningDate;
    private double salary;
    private String qualification;
    private List<String> subjects;
    private String status;
    private String username;
    
    public Faculty() {
        this.subjects = new ArrayList<>();
    }
    
    public Faculty(String facultyId, String firstName, String lastName, 
                  String email, String phone, String department, 
                  String designation, String qualification, double salary) {
        this.facultyId = facultyId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.designation = designation;
        this.qualification = qualification;
        this.salary = salary;
        this.joiningDate = LocalDate.now();
        this.status = "Active";
        this.subjects = new ArrayList<>();
        this.username = null;
    }
    
    // Getters and Setters
    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFullName() { return firstName + " " + lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
    
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    
    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

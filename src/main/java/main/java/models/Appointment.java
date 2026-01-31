package main.java.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class Appointment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED
    }

    private long appointmentId;
    private String studentId; // Mapping to student_code
    private String facultyId; // Mapping to instructor_code
    private Long officeHourId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Status status;
    private String purpose;
    private String rejectionReason;
    private LocalDateTime createdAt;

    // Transients
    private String studentName;
    private String facultyName;

    public Appointment() {
    }

    public long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public Long getOfficeHourId() {
        return officeHourId;
    }

    public void setOfficeHourId(Long officeHourId) {
        this.officeHourId = officeHourId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Appointment))
            return false;
        Appointment that = (Appointment) o;
        return appointmentId == that.appointmentId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(appointmentId);
    }
}

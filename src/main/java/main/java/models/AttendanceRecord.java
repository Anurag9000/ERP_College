package main.java.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Attendance entry for a section on a specific date.
 */
public class AttendanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sectionId;
    private LocalDate date;
    private Map<String, Boolean> attendanceByStudent;

    public AttendanceRecord() {
        this.attendanceByStudent = new HashMap<>();
    }

    public AttendanceRecord(String sectionId, LocalDate date) {
        this.sectionId = sectionId;
        this.date = date;
        this.attendanceByStudent = new HashMap<>();
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Map<String, Boolean> getAttendanceByStudent() {
        return attendanceByStudent;
    }

    public void setAttendanceByStudent(Map<String, Boolean> attendanceByStudent) {
        this.attendanceByStudent = attendanceByStudent;
    }

    public void markAttendance(String studentId, boolean present) {
        attendanceByStudent.put(studentId, present);
    }

    public double getAttendancePercentage() {
        if (attendanceByStudent.isEmpty()) {
            return 100.0;
        }
        long present = attendanceByStudent.values().stream().filter(Boolean::booleanValue).count();
        return (present * 100.0) / attendanceByStudent.size();
    }
}

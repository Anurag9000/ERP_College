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

    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE
    }

    private String sectionId;
    private LocalDate date;
    private Map<String, AttendanceStatus> statusByStudent;

    public AttendanceRecord() {
        this.statusByStudent = new HashMap<>();
    }

    public AttendanceRecord(String sectionId, LocalDate date) {
        this.sectionId = sectionId;
        this.date = date;
        this.statusByStudent = new HashMap<>();
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

    public Map<String, AttendanceStatus> getStatusByStudent() {
        return statuses();
    }

    public void setStatusByStudent(Map<String, AttendanceStatus> statusByStudent) {
        if (statusByStudent == null) {
            this.statusByStudent = new HashMap<>();
        } else {
            this.statusByStudent = new HashMap<>(statusByStudent);
        }
    }

    /**
     * Legacy accessor for boolean attendance maps. Converts internally stored statuses to booleans.
     * @deprecated Prefer {@link #getStatusByStudent()}.
     */
    @Deprecated
    public Map<String, Boolean> getAttendanceByStudent() {
        Map<String, Boolean> snapshot = new HashMap<>();
        statuses().forEach((studentId, status) ->
                snapshot.put(studentId, status != AttendanceStatus.ABSENT));
        return snapshot;
    }

    /**
     * Legacy mutator accepting boolean presence, translates into status map.
     * @deprecated Prefer {@link #setStatusByStudent(Map)}.
     */
    @Deprecated
    public void setAttendanceByStudent(Map<String, Boolean> attendanceByStudent) {
        Map<String, AttendanceStatus> map = statuses();
        map.clear();
        if (attendanceByStudent != null) {
            attendanceByStudent.forEach(this::markAttendance);
        }
    }

    public void markStatus(String studentId, AttendanceStatus status) {
        statuses().put(studentId, status);
    }

    public void markAttendance(String studentId, boolean present) {
        markStatus(studentId, present ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
    }

    public AttendanceStatus getStatus(String studentId) {
        return statuses().get(studentId);
    }

    public double getAttendancePercentage() {
        if (statuses().isEmpty()) {
            return 100.0;
        }
        long present = statuses().values().stream()
                .filter(status -> status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE)
                .count();
        return (present * 100.0) / statuses().size();
    }

    public long getTardyCount() {
        return statuses().values().stream()
                .filter(status -> status == AttendanceStatus.LATE)
                .count();
    }

    private Map<String, AttendanceStatus> statuses() {
        if (statusByStudent == null) {
            statusByStudent = new HashMap<>();
        }
        return statusByStudent;
    }
}

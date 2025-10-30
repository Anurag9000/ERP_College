package main.java.models;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a scheduled teaching section of a course.
 */
public class Section implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sectionId;
    private String courseId;
    private String title;
    private String facultyId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private int capacity;
    private LocalDate enrollmentDeadline;
    private LocalDate dropDeadline;
    private String semester;
    private int year;

    private final List<String> enrolledStudentIds;
    private final List<String> waitlistedStudentIds;

    public Section() {
        this.enrolledStudentIds = new ArrayList<>();
        this.waitlistedStudentIds = new ArrayList<>();
    }

    public Section(String sectionId, String courseId, String title, String facultyId,
                   DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                   String location, int capacity) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.title = title;
        this.facultyId = facultyId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.capacity = capacity;
        this.enrollmentDeadline = LocalDate.now().plusDays(14);
        this.dropDeadline = LocalDate.now().plusDays(28);
        this.semester = "Fall";
        this.year = LocalDate.now().getYear();
        this.enrolledStudentIds = new ArrayList<>();
        this.waitlistedStudentIds = new ArrayList<>();
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
       this.facultyId = facultyId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocalDate getEnrollmentDeadline() {
        return enrollmentDeadline;
    }

    public void setEnrollmentDeadline(LocalDate enrollmentDeadline) {
        this.enrollmentDeadline = enrollmentDeadline;
    }

    public LocalDate getDropDeadline() {
        return dropDeadline;
    }

    public void setDropDeadline(LocalDate dropDeadline) {
        this.dropDeadline = dropDeadline;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<String> getEnrolledStudentIds() {
        return enrolledStudentIds;
    }

    public List<String> getWaitlistedStudentIds() {
        return waitlistedStudentIds;
    }

    public int getAvailableSeats() {
        return Math.max(0, capacity - enrolledStudentIds.size());
    }

    public boolean isFull() {
        return enrolledStudentIds.size() >= capacity;
    }

    public boolean hasStudent(String studentId) {
        return enrolledStudentIds.contains(studentId) || waitlistedStudentIds.contains(studentId);
    }

    public void enrollStudent(String studentId) {
        if (!enrolledStudentIds.contains(studentId)) {
            enrolledStudentIds.add(studentId);
        }
    }

    public void waitlistStudent(String studentId) {
        if (!waitlistedStudentIds.contains(studentId)) {
            waitlistedStudentIds.add(studentId);
        }
    }

    public void removeStudent(String studentId) {
        enrolledStudentIds.remove(studentId);
        waitlistedStudentIds.remove(studentId);
    }

    public String promoteNextWaitlisted() {
        if (waitlistedStudentIds.isEmpty()) {
            return null;
        }
        String nextStudent = waitlistedStudentIds.remove(0);
        enrollStudent(nextStudent);
        return nextStudent;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Section)) {
            return false;
        }
        Section other = (Section) obj;
        return Objects.equals(sectionId, other.sectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionId);
    }
}

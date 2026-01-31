package main.java.models;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class OfficeHour implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private long officeHourId;
    private String facultyId; // Mapping to instructor_code
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;

    public OfficeHour() {
    }

    public long getOfficeHourId() {
        return officeHourId;
    }

    public void setOfficeHourId(long officeHourId) {
        this.officeHourId = officeHourId;
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

    public String getDisplayText() {
        return String.format("%s %s - %s @ %s",
                dayOfWeek.name(), startTime, endTime, location != null ? location : "Office");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OfficeHour))
            return false;
        OfficeHour that = (OfficeHour) o;
        return officeHourId == that.officeHourId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(officeHourId);
    }
}

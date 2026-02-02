package main.java.service;

import main.java.data.dao.AppointmentDao;
import main.java.models.Appointment;
import main.java.models.Faculty;
import main.java.models.OfficeHour;
import main.java.models.Section;
import main.java.utils.DatabaseUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling faculty interactions and availability.
 */
public class FacultyService {

    private static AppointmentDao appointmentDao = new AppointmentDao();

    public static void setAppointmentDao(AppointmentDao dao) {
        appointmentDao = dao;
    }

    public enum StatusType {
        TEACHING, IN_OFFICE, FREE, UNAVAILABLE
    }

    public static class CurrentStatus {
        public StatusType type;
        public String location;
        public String description;

        public CurrentStatus(StatusType type, String location, String description) {
            this.type = type;
            this.location = location;
            this.description = description;
        }
    }

    public static List<Faculty> getAllFaculty() {
        return DatabaseUtil.getAllFaculty(); // Returns List<Faculty>
    }

    /**
     * Gets office hours for a faculty member.
     * 
     * @param facultyId the faculty ID (must not be null or empty)
     * @return list of office hours, never null
     * @throws IllegalArgumentException if facultyId is null or empty
     */
    public static List<OfficeHour> getOfficeHours(String facultyId) {
        if (facultyId == null || facultyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        return appointmentDao.getOfficeHours(facultyId);
    }

    /**
     * Books an appointment.
     * 
     * @param apt the appointment (must not be null)
     * @throws IllegalArgumentException if apt is null
     */
    public static void bookAppointment(Appointment apt) {
        if (apt == null) {
            throw new IllegalArgumentException("Appointment cannot be null");
        }
        appointmentDao.insertAppointment(apt);
    }

    /**
     * Gets appointments for a student.
     * 
     * @param studentId the student ID (must not be null or empty)
     * @return list of appointments, never null
     * @throws IllegalArgumentException if studentId is null or empty
     */
    public static List<Appointment> getStudentAppointments(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        return appointmentDao.getAppointmentsForStudent(studentId);
    }

    /**
     * Determines where a professor is right now.
     */
    /**
     * Determines where a professor is right now.
     * 
     * @param facultyId the faculty ID (must not be null or empty)
     * @return the current status
     * @throws IllegalArgumentException if facultyId is null or empty
     */
    public static CurrentStatus FacultyCurrentStatus(String facultyId) {
        if (facultyId == null || facultyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        // Mock time for demo or use real
        LocalTime now = LocalTime.now();
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        // 1. Check Classes (Teaching)
        List<Section> allSections = DatabaseUtil.getAllSections();
        for (Section s : allSections) {
            // Null check: facultyId might be null in some sections
            if (s.getFacultyId() != null && s.getFacultyId().equals(facultyId)) {
                if (s.getDayOfWeek() == today) {
                    LocalTime start = s.getStartTime();
                    LocalTime end = s.getEndTime();
                    if (start != null && end != null && !now.isBefore(start) && now.isBefore(end)) {
                        return new CurrentStatus(StatusType.TEACHING, s.getLocation(), "Teaching " + s.getCourseId());
                    }
                }
            }
        }

        // 2. Check Office Hours
        List<OfficeHour> officeHours = appointmentDao.getOfficeHours(facultyId);
        for (OfficeHour oh : officeHours) {
            if (oh.getDayOfWeek() == today) {
                if (!now.isBefore(oh.getStartTime()) && now.isBefore(oh.getEndTime())) {
                    return new CurrentStatus(StatusType.IN_OFFICE, oh.getLocation(), "Office Hours");
                }
            }
        }

        // 3. Else Free
        return new CurrentStatus(StatusType.FREE, "Unknown", "Available");
    }
}

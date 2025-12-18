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

    public static List<OfficeHour> getOfficeHours(String facultyId) {
        return appointmentDao.getOfficeHours(facultyId);
    }

    public static void bookAppointment(Appointment apt) {
        appointmentDao.insertAppointment(apt);
    }

    public static List<Appointment> getStudentAppointments(String studentId) {
        return appointmentDao.getAppointmentsForStudent(studentId);
    }

    /**
     * Determines where a professor is right now.
     */
    public static CurrentStatus FacultyCurrentStatus(String facultyId) {
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

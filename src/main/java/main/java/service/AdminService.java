package main.java.service;

import main.java.models.Faculty;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;

import java.time.LocalDate;

/**
 * Admin-only helper operations (user provisioning, settings, assignments).
 */
public final class AdminService {

    private AdminService() {
    }

    public static void ensureAdmin(User actor) {
        if (actor == null || !"Admin".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Administrator privileges required.");
        }
    }

    public static User createUser(User actor, String username, String role, String fullName, String email, String tempPassword) {
        ensureAdmin(actor);
        return DatabaseUtil.addUser(username, role, fullName, email, tempPassword);
    }

    public static void linkStudentProfile(User actor, String username, Student studentProfile) {
        ensureAdmin(actor);
        studentProfile.setUsername(username);
        if (studentProfile.getAdmissionDate() == null) {
            studentProfile.setAdmissionDate(LocalDate.now());
        }
        if (DatabaseUtil.getStudent(studentProfile.getStudentId()) == null) {
            DatabaseUtil.addStudent(studentProfile);
        } else {
            DatabaseUtil.updateStudent(studentProfile);
        }
    }

    public static void linkFacultyProfile(User actor, String username, Faculty facultyProfile) {
        ensureAdmin(actor);
        facultyProfile.setUsername(username);
        if (facultyProfile.getJoiningDate() == null) {
            facultyProfile.setJoiningDate(LocalDate.now());
        }
        if (DatabaseUtil.getFaculty(facultyProfile.getFacultyId()) == null) {
            DatabaseUtil.addFaculty(facultyProfile);
        } else {
            DatabaseUtil.updateFaculty(facultyProfile);
        }
    }

    public static void toggleMaintenance(User actor, boolean maintenanceOn) {
        ensureAdmin(actor);
        DatabaseUtil.setMaintenanceMode(maintenanceOn);
    }
}

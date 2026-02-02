package main.java.service;

import main.java.models.EnrollmentRecord;
import main.java.models.Faculty;
import main.java.models.MaintenanceWindow;
import main.java.models.NotificationRequest;
import main.java.models.Student;
import main.java.models.User;
import main.java.utils.DatabaseUtil;
import main.java.utils.AuditLogService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * Creates a new user account.
     * 
     * @param actor        the admin user (must not be null)
     * @param username     the username (must not be null or empty)
     * @param role         the role (must not be null or empty)
     * @param fullName     the full name (can be null)
     * @param email        the email (can be null)
     * @param tempPassword the temporary password (must not be null or empty)
     * @return the created user
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SecurityException        if actor is not admin
     */
    public static User createUser(User actor, String username, String role, String fullName, String email,
            String tempPassword) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        if (tempPassword == null || tempPassword.isEmpty()) {
            throw new IllegalArgumentException("Temporary password cannot be null or empty");
        }
        ensureAdmin(actor);
        return DatabaseUtil.addUser(username, role, fullName, email, tempPassword);
    }

    /**
     * Links a student profile to a user account.
     * 
     * @param actor          the admin user (must not be null)
     * @param username       the username (must not be null or empty)
     * @param studentProfile the student profile (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SecurityException        if actor is not admin
     */
    public static void linkStudentProfile(User actor, String username, Student studentProfile) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (studentProfile == null) {
            throw new IllegalArgumentException("Student profile cannot be null");
        }
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

    /**
     * Links a faculty profile to a user account.
     * 
     * @param actor          the admin user (must not be null)
     * @param username       the username (must not be null or empty)
     * @param facultyProfile the faculty profile (must not be null)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws SecurityException        if actor is not admin
     */
    public static void linkFacultyProfile(User actor, String username, Faculty facultyProfile) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (facultyProfile == null) {
            throw new IllegalArgumentException("Faculty profile cannot be null");
        }
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
        DatabaseUtil.setMaintenanceMode(actor, maintenanceOn);
    }

    /**
     * Updates a user's profile information.
     * 
     * @param actor    the admin user (must not be null)
     * @param username the username (must not be null or empty)
     * @param fullName the full name (can be null)
     * @param email    the email (can be null)
     * @throws IllegalArgumentException if username is null or empty
     * @throws SecurityException        if actor is not admin
     */
    public static void updateUserProfile(User actor, String username, String fullName, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        ensureAdmin(actor);
        DatabaseUtil.updateUserContact(username, fullName, email);
        AuditLogService.log(AuditLogService.EventType.USER_MANAGEMENT,
                actor.getUsername(),
                "Updated profile for " + username);
    }

    public static void updateUserRole(User actor, String username, String role) {
        ensureAdmin(actor);
        DatabaseUtil.updateUserRole(username, role);
        AuditLogService.log(AuditLogService.EventType.USER_MANAGEMENT,
                actor.getUsername(),
                "Changed role for " + username + " to " + role);
    }

    public static void setUserActive(User actor, String username, boolean active) {
        ensureAdmin(actor);
        DatabaseUtil.setUserActive(username, active);
        AuditLogService.log(AuditLogService.EventType.USER_MANAGEMENT,
                actor.getUsername(),
                (active ? "Reactivated " : "Suspended ") + username);
    }

    public static List<AuditLogService.AuditEvent> auditTrailForUser(String username) {
        return AuditLogService.recentEventsForUser(username);
    }

    /**
     * Updates course relationships (prerequisites, corequisites, antirequisites).
     * 
     * @param actor          the admin user (must not be null)
     * @param courseId       the course ID (must not be null or empty)
     * @param prerequisites  list of prerequisite course IDs (can be null)
     * @param corequisites   list of corequisite course IDs (can be null)
     * @param antirequisites list of antirequisite course IDs (can be null)
     * @throws IllegalArgumentException if courseId is null or empty
     * @throws SecurityException        if actor is not admin
     */
    public static void updateCourseRelationships(User actor,
            String courseId,
            List<String> prerequisites,
            List<String> corequisites,
            List<String> antirequisites) {
        if (courseId == null || courseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Course ID cannot be null or empty");
        }
        ensureAdmin(actor);
        DatabaseUtil.updateCoursePrerequisites(courseId, prerequisites);
        DatabaseUtil.updateCourseCorequisites(courseId, corequisites);
        DatabaseUtil.updateCourseAntirequisites(courseId, antirequisites);
        AuditLogService.log(AuditLogService.EventType.USER_MANAGEMENT,
                actor.getUsername(),
                "Updated catalog relationships for " + courseId);
    }

    public static EnrollmentRecord overrideEnroll(User actor,
            String studentId,
            String sectionId,
            boolean ignoreCapacity,
            boolean ignoreConflicts,
            boolean ignoreRequisites,
            boolean ignoreCredits) {
        ensureAdmin(actor);
        return DatabaseUtil.overrideEnrollStudent(actor, studentId, sectionId,
                ignoreCapacity, ignoreConflicts, ignoreRequisites, ignoreCredits);
    }

    public static void updateSectionDeadlines(User actor,
            String sectionId,
            LocalDate enrollmentDeadline,
            LocalDate dropDeadline) {
        ensureAdmin(actor);
        DatabaseUtil.updateSectionDeadlines(sectionId, enrollmentDeadline, dropDeadline);
        AuditLogService.log(AuditLogService.EventType.ENROLLMENT_CHANGE,
                actor.getUsername(),
                "Updated deadlines for " + sectionId);
    }

    public static void promoteWaitlisted(User actor, String sectionId, String studentId) {
        ensureAdmin(actor);
        DatabaseUtil.promoteWaitlistedStudent(actor, sectionId, studentId);
    }

    public static void removeWaitlistEntry(User actor, String sectionId, String studentId) {
        ensureAdmin(actor);
        DatabaseUtil.removeWaitlistEntry(actor, sectionId, studentId);
    }

    public static MaintenanceWindow scheduleMaintenanceWindow(User actor,
            LocalDateTime start,
            LocalDateTime end,
            String message) {
        ensureAdmin(actor);
        return DatabaseUtil.scheduleMaintenanceWindow(actor, start, end, message);
    }

    public static void cancelMaintenanceWindow(User actor, long windowId) {
        ensureAdmin(actor);
        DatabaseUtil.cancelMaintenanceWindow(actor, windowId);
    }

    public static List<MaintenanceWindow> getMaintenanceWindows(User actor) {
        ensureAdmin(actor);
        return DatabaseUtil.getMaintenanceWindows();
    }

    public static Optional<MaintenanceWindow> getNextMaintenanceWindow(User actor) {
        ensureAdmin(actor);
        return DatabaseUtil.getNextMaintenanceWindow();
    }

    public static void broadcastNotification(User actor, NotificationRequest request) {
        ensureAdmin(actor);
        DatabaseUtil.broadcastNotification(actor, request);
    }
}

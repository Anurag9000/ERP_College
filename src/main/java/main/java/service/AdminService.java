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

    public static User createUser(User actor, String username, String role, String fullName, String email,
            String tempPassword) {
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
        DatabaseUtil.setMaintenanceMode(actor, maintenanceOn);
    }

    public static void updateUserProfile(User actor, String username, String fullName, String email) {
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

    public static void updateCourseRelationships(User actor,
            String courseId,
            List<String> prerequisites,
            List<String> corequisites,
            List<String> antirequisites) {
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
